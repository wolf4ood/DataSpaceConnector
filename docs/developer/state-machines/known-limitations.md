# Known limitations of the state machines

The control plane drives transfer processes, contract negotiations and policy monitor entries through one of two state
machines:

- the **poll-based** state machine, which periodically leases a batch of entities per state from the store;
- the **task-based** state machine, which processes a task published to NATS for every state change.

Both were reviewed for robustness and for running several replicas against the same database ([#6105] and [#6116]).
The issues found were either fixed or, when their impact is low or a fix is not worth its cost, accepted. This page
lists the accepted ones: what happens, when, the impact, and how to mitigate it where possible. See also the
[stateful entity versioning] decision record, which describes how leases and versions protect the entities.

## Common to both state machines

- **Synchronized clocks.** Lease expiry and retry delays compare timestamps written by one replica with the clock of
  another. Replicas are expected to keep their clocks synchronized (e.g. with NTP).
- **Duplicate protocol messages.** A step can run twice: on the poll-based state machine, when the lease of an entity
  expires while it is still processed; on the task-based one, when a task is delivered again after a failure. The
  counter-party can then receive the same message twice, with a different message id. Counter-parties are expected
  to handle duplicate messages.
- **No timeout for waiting states.** A transfer process or a contract negotiation waiting for a message of the
  counter-party (e.g. REQUESTED, AGREED) waits indefinitely.
- **Agreement rebuilt on every attempt.** Until the contract agreement is saved, the provider builds a new one (new
  id, new signing date) at every attempt to send it. If an attempt reaches the consumer but is seen as failed, the
  consumer receives a second agreement: it replaces the first one while in AGREED (the `agreed` event fires twice),
  or rejects it if it has moved on, which terminates the negotiation.
- **Retirement reverted before finalization.** Retiring an agreement does not change the version of its negotiation,
  and saving a negotiation writes the retirement fields of its agreement. A retirement done while the negotiation is
  between AGREED and FINALIZED can be reverted by a concurrent save of an older copy. Once the negotiation is
  FINALIZED, nothing saves it anymore.
- **Saving a deleted entity re-inserts it.** The store upsert inserts a missing row whatever the entity version is.
  This is not reachable today: negotiations are deleted only in TERMINATED without agreement, which nothing saves
  anymore, and transfer processes are never deleted. Splitting `save` into a create and an update would remove it.
- **State changed before a step that can fail.** `processProviderInitial` transitions the transfer process before
  storing the data address of the data plane; a retryable failure of that store would make the retry transition fail.
  It is not reachable with `VaultDataAddressStore`, whose failures are fatal.

## Poll-based state machine

- **Lease duration.** The lease must last longer than the slowest processing step of an entity, including the HTTP
  timeouts and retries of a protocol message: otherwise the entity is processed again, by another replica or by the
  same one. It is configured with `edc.transfer.state-machine.lease-duration`,
  `edc.negotiation.state-machine.lease-duration` and `edc.policy.monitor.lease-duration` (default `PT60S`).
- **Synchronous failures are not retries.** An entity whose processing throws before anything is saved (e.g. an
  unavailable vault, inconsistent data) does not increase its state count: it is not subject to the retry limit or
  to the retry delay, and it is processed again at every iteration, with a severe log each time. This costs load and
  logs, not correctness.
- **Retry delays block the batch.** Entities waiting for their retry delay are leased first, ordered by state
  timestamp, and fill the batch: ready entities in the same state are not processed until the delays end (up to a few
  minutes with the defaults), and every waiting entity costs a lease and its release at every iteration. It shows when
  at least `batch-size` entities in one state are waiting, e.g. during the outage of a counter-party, which then delays
  the processing of the others. A fix would persist the due time on the entity and filter on it.
- **Scalability.**
  - **Idle polling:** an idle replica runs one query per processor (state) at every iteration, with no idle backoff.
  - **Leasing a batch:** it takes one UPDATE per entity.
  - **No backpressure:** nothing limits the number of dispatches in flight.

## Task-based state machine (NATS)

- **Provisioning.** The streams and consumers created by the subscribers (`edc.nats.*.subscriber.autocreate`) are
  meant for testing: memory storage, limits retention and the server defaults. In production, provision them with
  persistent storage and the following settings:
  - **`MaxAckPending`:** size it for the tasks that can wait at the same time. A task waiting for a retry delay counts
    toward it, and the limit is shared by all the replicas, which use the same durable consumer. When it is reached,
    new tasks are not delivered until some delays expire.
  - **Ack wait:** the subscriber marks the messages it holds as in progress at a third of the ack wait, so the ack wait
    does not need to cover the processing time.
- **Task store.** The default in-memory task store does not work with several replicas: use the SQL task store.
- **Retries.** A retryable failure is delivered again after `edc.transfer.send.retry.base-delay.ms` /
  `edc.negotiation.send.retry.base-delay.ms`, doubled at every delivery, up to `edc.transfer.send.retry.limit` /
  `edc.negotiation.send.retry.limit`, as on the poll-based state machine. There is no maximum delay: a high limit leads
  to very long delays.
- **Lost tasks.** A task is published before its transaction commits. A replica that does not see it yet retries with
  a growing delay and drops the message after `edc.nats.*.subscriber.max-retries` deliveries, so a commit slower than
  that window loses the message. A task whose handling keeps failing is terminated after the retry limit, with a
  severe log. In both cases, and if messages are lost on the NATS side (e.g. a purged stream), the task stays in the
  task store, but nothing publishes it again.
- **Transaction scope.** The handling of a task runs in a database transaction that holds a connection while the
  remote call is made. The save that follows the call runs on the HTTP client thread, in its own transaction,
  together with the creation of the next task. The two are not atomic, which is safe: a leftover task finds the entity
  in another state and is skipped.
- **Lease collisions.** A task whose save collides with the lease held by an incoming protocol message or an API
  command is retried with the usual delay: on redelivery, the entity has either moved on, and the task is skipped, or
  the remote call is made again. These retries do not count toward the retry limit. Such leases are held only for the
  duration of the message or command, so collisions are rare and short.
- **Head-of-line blocking.** A replica processes the tasks of an entity type with one subscriber thread, one message
  at a time, and tasks are not scoped by participant: a slow counter-party delays the tasks behind it on that replica.
  To be addressed by processing the batch with a pool of workers.
- **Stale tasks.** A task carries only the entity id and its state. A task waiting for a retry delay while its entity
  leaves the state and enters it again is processed for the new visit, which has its own task: at worst, a remote
  call is made twice. The entity version cannot tell them apart, since every save changes it, including the one of a
  failed attempt.

[#6105]: https://github.com/eclipse-edc/Connector/issues/6105
[#6116]: https://github.com/eclipse-edc/Connector/issues/6116
[stateful entity versioning]: ../decision-records/2026-10-14-stateful-entity-versioning/README.md

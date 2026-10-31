# MARS-OPS — Autonomous Mars Colony Management & Emergency Simulator

Programming II · Lab 5 · Team: (names)

> Work in progress. The full plan is in `MARS-OPS-MASTER.pdf`.

| # | Section | Author |
|---|---|---|
| R0 | Title, team, how to compile and run, how to run scenarios and tests | C |
| R1 | System description and main components | A |
| R2 | Relationships: why chosen, how in UML, how in Java | A |
| R3 | Agents and facilities decisions | A |
| R4 | Resource allocation rules | B |
| R5 | Mission states and rules | B |
| R6 | Environmental effects (incl. missions already in progress) | B |
| R7 | Emergency rules and requirements | C |
| R8 | Time and simulation | C |
| R9 | Demonstration scenarios | C |
| R10 | Other decisions + testing instructions | C |


## Resource allocation (R4)

Every resource lives inside a facility, so the colony never stores a single global
number. When we say "the colony has 550 oxygen", that figure is just the sum of what
each facility holds right now.

- **Where resources come from (D-01).** Storage facilities are used first. If they
  cannot cover the request, we continue with the other operational facilities in the
  order they were added to the colony. For example, a request for 450 oxygen empties
  the Storage (200), then takes 250 from Habitat A, and Lab 1 is never touched.
- **What counts (D-02).** Only facilities that are `OPERATIONAL` contribute to the
  totals. A damaged, unavailable or destroyed facility counts as zero, even though
  the stock inside it is still there. That is why a fire in a lab immediately lowers
  the colony-wide total.
- **All or nothing (D-03).** Before anything is withdrawn, we check every resource
  the mission needs. If even one is short, nothing is consumed and the mission is
  rejected with a clear message such as `Insufficient OXYGEN: need 600, available 550`.
  Resources are taken once, when the mission is accepted. If the mission fails
  later, they are not given back.

The policy is a separate class (`AllocationPolicy`), so another strategy can be
plugged in without touching `ResourceManager`.

## Mission states and rules (R5)

A mission moves through five states:
`CREATED → ASSIGNED → IN_PROGRESS → COMPLETED`, and from any of the first three it
can go to `FAILED`. `COMPLETED` and `FAILED` are final. A failed mission is never
retried; if you want to try again, you create a new `Mission`.

- **Time (D-04).** The first tick after acceptance moves the mission from `ASSIGNED`
  to `IN_PROGRESS`, and that same tick counts as the first unit of work. So a
  3-unit mission finishes on the third tick.
- **Who can take part (D-07).** A mission is accepted only if every agent is able to
  perform it, every agent is available, and together the team covers all the required
  capabilities.

When a mission is submitted, `MarsColony` runs these checks in order and stops at the
first one that fails:

| Step | Check | Reason given |
|------|-------|--------------|
| S0 | The mission is still `CREATED` | Mission was already processed |
| S1 | No other mission is active | Another mission is already active (id) |
| S2 | At least one agent was given | No agents were assigned to the mission |
| S3 | The environment and target facility allow it | The mission's own reason, e.g. the dust storm message |
| S4 | A communication station works, if the mission needs one | No operational communication station |
| S5 | Each agent is in the colony, controllable, able and available | The agent's own reason |
| S6 | The team covers every required capability | No assigned agent provides capability X |
| S7 | The resources are enough | Insufficient X: need N, available M |
| S8 | Accept: consume resources, assign the team, make it active | (accepted) |

A rejected mission is marked `FAILED`, added to the history, and never becomes
active. It consumes nothing. The only exception is S0: a mission that was already
processed is rejected without being failed again.

## Environmental effects (R6)

The environment can be `NORMAL`, `DUST_STORM` or `COMMUNICATION_DISRUPTION`. The
effects are decided by the missions themselves, not by the colony, so a new mission
type can define its own reaction.

- **Dust storm.** Surface missions (exploration) cannot start. A surface mission that
  is already running fails immediately with a dust storm reason. Missions that do not
  go outside, such as research, keep running.
- **Communication disruption.** Rescue missions cannot start, and a running rescue
  mission fails. Remote-controlled agents (drones) cannot be assigned while the
  link is down. Other missions continue normally.

**What happens to missions already in progress (D-06).** The rules are applied at the
exact moment `changeEnvironment(...)` is called, not at the next tick. If the new
condition makes the running mission impossible, it fails right away, its agents are
released, and the ones that are still usable go back to their home location. A
damaged rover stays where it is.
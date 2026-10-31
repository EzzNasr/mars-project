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
## Emergency rules (R7)

There are four emergencies: oxygen leak, fire, rover breakdown and medical emergency.
None of them is random. We start them by hand (`triggerEmergency`) or schedule them
for a time unit (`scheduleEmergency`), so every run gives the same result.

Each emergency class knows what it needs and what it does. `EmergencyManager` runs
the same steps for all of them and never checks the class of an agent: it only asks
the agent (`isAvailable`, `canHandle`, `hasCapability`).

| Emergency | Responders (one different agent each) | Resources used when fixed |
|---|---|---|
| Oxygen leak | ENGINEERING + ROBOTIC_REPAIR | 2 SPARE_PARTS, 5 BATTERY_POWER |
| Fire | ENGINEERING + MEDICAL | 10 WATER, 1 MEDICAL_SUPPLIES, 3 BATTERY_POWER |
| Rover breakdown | ENGINEERING + ROBOTIC_REPAIR | 2 SPARE_PARTS, 5 BATTERY_POWER |
| Medical emergency | MEDICAL | 2 MEDICAL_SUPPLIES, 5 OXYGEN |

**What happens in `handle` (D-10 to D-12).**

1. The damage happens first, as soon as the emergency is detected.
2. We check the special conditions of that emergency (for example, a medical
   emergency in a facility that is not working fails here).
3. For every needed capability we take the first free agent that can handle it. If
   nobody is found the emergency is `UNRESOLVED` with `No available responder with capability X`.
4. We check the resources. If something is short it is `UNRESOLVED` and nothing is consumed.
5. Otherwise the team goes to the emergency location, the resources are consumed and
   the emergency is `RESOLVED`. The team is released but **stays there**.

An unresolved emergency is never tried again.

**What each one does.**

| Emergency | When detected | If fixed | If not fixed |
|---|---|---|---|
| Oxygen leak (D-13) | habitat leaks and loses the oxygen | leak stops, the loss was only once | the same amount is lost again, never below 0 |
| Fire (D-14) | lab `UNAVAILABLE`, equipment damaged, 5 oxygen burns, people inside lose 30 health | lab `DAMAGED`, fire out, people inside recover 20 | lab `DESTROYED`, its stock stops counting |
| Rover breakdown (D-15) | rover `DAMAGED` where it stands, the active mission fails if the rover was in it | rover repaired and sent home | rover stays broken |
| Medical emergency (D-16) | patient loses `severity` health | patient recovers 40 | nothing more |

The patient can never treat himself. Emergencies do not stop the active mission,
except the rover breakdown that kills the mission of that rover (D-17).

## Time and simulation (R8)

`Simulation` is only the clock. It moves time forward and calls the colony and the
emergency manager in a fixed order, the rules themselves live in those classes.

Every time unit (`advanceOneUnit`) does this, in this order:

1. time goes up by one;
2. the environment changes scheduled for this time are applied (a running mission is checked right away, D-06);
3. the colony ticks: agents update fatigue and battery, then the mission starts or advances (D-04, D-09);
4. the emergencies scheduled for this time are handled.

So a dust storm scheduled for time 2 already stops a rover mission in unit 2, and an
emergency scheduled for time 3 sees the colony after the tick of unit 3.

- `advanceTime(n)` repeats one unit n times. A negative number throws `IllegalArgumentException`.
- `scheduleEnvironmentChange` and `scheduleEmergency` need `atTime >= 1`. Each scheduled
  item fires once, at that exact time. Two items at the same time fire in the order they were added.
- `triggerEmergency` handles an emergency right now and does not move the clock.
- `report()` returns the colony report for the current time (Simulation does not print).

## Demonstration scenarios (R9)

All five scenarios are in `Main`, and `java -cp out marsops.Main` runs them in order.
Each one builds a new sample colony (see R10), prints a heading, the colony report
before and after, the outcome and one `[OK]` or `[FAIL]` line per check. At the end it
prints `ALL SCENARIOS PASSED` or the list of the failed ones. Every scenario is also a
public method that returns `true` when all its checks passed, so the tests use them too.

| # | What we do | What we see |
|---|---|---|
| 1 Successful exploration | Mission M-1 (Valles, 3 units, needs 10 FUEL and 5 BATTERY_POWER) with Dr. Layla and Rover-1, then `advanceTime(3)` | accepted, `ASSIGNED`, then `COMPLETED` after 3 ticks. Fuel 140 to 130, both agents back home, Rover-1 battery 85, Dr. Layla fatigue 30 |
| 2 Mission rejection | The same mission but with Dr. Salma (medical) instead of Dr. Layla | prints `Mission cannot be assigned.` and `Reason: Dr. Salma does not possess the required capability`. Mission `FAILED`, resources unchanged |
| 3 Resource shortage | Research mission M-3 in Lab 1 that needs 600 OXYGEN (the colony has 550) | rejected with `Insufficient OXYGEN: need 600, available 550`, all totals unchanged |
| 4a Oxygen leak fixed | Leak of 80 in Habitat A by `triggerEmergency` | `RESOLVED`, Habitat A oxygen 300 to 220, 2 SPARE_PARTS and 5 BATTERY_POWER used, Omar and Fixer-1 now in Habitat A and free |
| 4b Oxygen leak not fixed | Omar and Fixer-1 are first put in a repair mission, then the same leak | `UNRESOLVED` (`No available responder with capability ENGINEERING`), oxygen 300 to 140, nothing consumed |
| 5a Storm blocks start | `changeEnvironment(DUST_STORM)`, then submit the scenario 1 mission | prints `Exploration Mission cannot start.` and `Reason: Surface operations are not allowed during a dust storm` |
| 5b Storm stops a mission | Accept the scenario 1 mission, `advanceTime(1)`, then the dust storm starts | mission `FAILED` right away with a dust storm reason, agents released |

## Other decisions and testing (R10)

**Things the spec did not say, so we decided:**

- The responder for each capability is the first suitable agent in the colony's list.
- The damage of an emergency happens when it is detected, before we know if anybody can help.
  That is why an unsolved oxygen leak loses oxygen twice.
- Responders are released after the response but stay at the emergency location (D-12).
  In a fire they are already inside the lab when it is fixed, so they also recover 20 (health stops at 100).
- A robot inside a burning lab becomes `DAMAGED` (30 damage is its limit), and the recovery of 20 does not repair it.
- An oxygen loss or a severity that is not positive throws `IllegalArgumentException`.
- An item scheduled for a time that already passed never fires.

**Assumptions D-01 to D-18 and where they are explained:**

| ID | Rule | Section |
|---|---|---|
| D-01 | Storage facilities are used first, then the others in the order they were added | R4 |
| D-02 | Only `OPERATIONAL` facilities count in the totals | R4 |
| D-03 | Resources are all-or-nothing, taken when the mission is accepted, never refunded | R4 |
| D-04 | The first tick after acceptance starts the mission and counts as unit 1 | R5 |
| D-05 | `COMPLETED` and `FAILED` are final, a failed mission is never retried | R5 |
| D-06 | Environment changes hit a running mission immediately | R6 |
| D-07 | Every agent must be able and available, and the team covers all capabilities | R5 |
| D-08 | Usable: health above 20, fatigue below 100, robot not damaged and battery above 0 | R3 |
| D-09 | Drain per tick: rover 5, drone 8, repair robot 3. Idle robots +1. Fatigue +10 working, -5 resting | R3 |
| D-10 | Responders needed for each emergency | R7 |
| D-11 | Resources needed for each emergency | R7 |
| D-12 | Responders must be available and stay at the emergency location. No automatic retry | R7 |
| D-13 | Oxygen leak: lost once if fixed, twice if not | R7 |
| D-14 | Fire: lab `DAMAGED` if fixed, `DESTROYED` if not | R7 |
| D-15 | Rover breakdown: rover `DAMAGED`, the active mission fails | R7 |
| D-16 | Medical emergency: patient recovers 40, never treats himself | R7 |
| D-17 | Emergencies do not stop the active mission (except the rover) | R7 |
| D-18 | Facility capacity is the maximum total units it can store | R3 |

**Sample colony used by the demos and tests** (Mars Base Alpha, facilities added in this order):

| Facility | Capacity | Stock |
|---|---|---|
| Habitat A | 1000 | OXYGEN 300, WATER 150, FOOD 150 |
| Storage | 2000 | OXYGEN 200, WATER 200, FOOD 200, FUEL 100, BATTERY_POWER 100, MEDICAL_SUPPLIES 20, SPARE_PARTS 10 |
| Lab 1 | 500 | OXYGEN 50, BATTERY_POWER 20, Microscope |
| Power 1 | 500 | FUEL 40, BATTERY_POWER 100 |
| Comm 1 | 200 | none |

Agents: Dr. Layla (scientist, GEOLOGY and CHEMISTRY), Eng. Omar (engineer), Dr. Salma
(medical officer), Rover-1 (carries a Spectrometer), Fixer-1 (repair robot) and Drone-1.
The humans live in Habitat A, the robots in Storage. Totals at the start: OXYGEN 550, WATER 350,
FOOD 350, FUEL 140, BATTERY_POWER 220, MEDICAL_SUPPLIES 20, SPARE_PARTS 10.

**Testing.** `EmergencySimulationTests` has the checks C01 to C20: the needs of each
emergency (C01), the oxygen leak fixed and not fixed (C02 to C05), who is chosen as
responder (C06, C07), the active mission not interrupted (C08), fire, rover and medical
emergency (C09 to C11), the simulation clock (C12 to C15), the history (C16) and the five
`Main` scenarios (C17 to C20).

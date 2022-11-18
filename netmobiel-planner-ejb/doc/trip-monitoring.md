# Trip Monitoring

[Back to Design](./design.md)

The Planner Service is monitoring the progress of a trip. A Trip has a state, this state is basically derived form the state of its constituting legs. The following State Transition Diagrams show relationship:

![Trip State Transition Diagram](Planner-Passenger-Trip-STD.png)

The STD of the Leg is similar, but has no decision points. 

![Leg State Transition Diagram](Planner-Passenger-Leg-STD.png)

In Netmobiel the Rideshare Service fares are paid used Netmobiel credits. At booking time the fare is reserved. Once the ride has completed, both passenger and driver are informed by a message to review the ride and to confirm (or deny) their presence. The current rules are:
* A time-out period of 7 days applies to the validation.
* If the driver and passenger both confirm, the fare is paid.
* If the driver confirms, but the passenger does not respond in time, the fare is paid.
* If the driver confirms, but the passenger denies then a dispute occurs. Both parties are informed by a message and asked to reconsider their decision.
* In all other cases the reservation of the fare is released to the passenger. No payment will be made.

These rules lead to the state transition diagram below:

![Trip Validation STD](Planner-Trip-Validation-STD.png)

The picture does not include the reminders sent to the driver and passenger. Also omitted is the reconsideration step. Both driver and passenger can roll-back their own decision after the validation, but only if that step is to their disadvantage. E.g., a driver can roll-back the validation if he/she thinks the payment of the fare to him/her was unjustified. The passenger can roll-back if he/she thinks the cancelling of the payment was unjustified.
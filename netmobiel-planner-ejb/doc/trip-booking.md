# Trip Booking

[Back to Design](./design.md)

The Planner Service iniates the booking, if necessary. Currently only the Rideshare Service rides require a booking. The booking process involves also other services like the [Overseer Service](../../netmobiel-overseer-ejb/doc/design.md) and the [Rideshare Service](../../netmobiel-rideshare-ejb/doc/design.md).

![Planner Create Booking Sequence Diagram](Planner-Create-Booking-Sequence-Diagram.png)

Note that the diagram show alternate paths: The case of an automatically confirmed booking and a explicitly confirmed booking.

To cancel a booking the following sequence of calls apply. 

![Planner Cancel Booking Sequence Diagram](Planner-Cancel-Booking-Sequence-Diagram.png)

This SD shows three possible flows for cancelling a booking:
1. Cancel booking by passenger in Netmobiel App.
2. Cancel booking by passenger in mobility provider's app
3. Cancel booking by mobility provider, e.g., the rideshare driver.
# Design
The Overseer is the coordinator and orchestrator of the Netmobiel backend. The Overseer listens to events and calls methods on session beans of the other services in Netmobiel, as depicted in this [component diagram](../../doc/design.md#backend).

## Processors
The Overseer uses *Processors* to structure the orchestration tasks. Below follows a summary of each *Processor*:
* **BookingProcessor**: Handles the trip booking process, in particular the communication with the Rideshare Service.
* **DelegationProcessor**: Handles the delegation binding process, i.e., the process to become a delegate for someone.
* **PaymentProcessor**: Handles the payment of fares and rewards: reservation, release, payment, refund.
* **QuoteProcessor**: Handles the conversion of euro's to Netmobiel credits.
* **ReportProcessor**: Handles the generation of reports by each service.
* **RewardProcessor**: Handles the handing out (or revoking) of rewards, including the evaluation.
* **ShoutOutProcessor**: Handles the processes around a shout-out.
* **TripProgressProcessor**: Handles the progress of a trip from *Scheduled* to *Completed*.
* **UserProcessor**: Handles the user synchronization in each service. 


## Reporting
For supporting the research a reporting facility has been added. Reports are generated on monthly basis at the beginning of each month. The number of months to lookback, start of the reporting period and the recipient email address are configured in the application server settings.
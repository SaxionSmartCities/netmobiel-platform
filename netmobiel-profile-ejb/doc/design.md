# Design
The Profile Service manages the personal data of the Netmobiel user. For ease of use each of the other services (most) have a synchronized subset of the profile. The exception is the communicator: Communicator-specific settings are stored by the communicator only. 

## Core Profile Management
Following below is the class diagram of the core of the Profile service.

![Profile Service Class Diagram](Profile-Service-Core-Class-Diagram.png)

Noteworthy to mention is the visible combination of the Passenger settings and the Rideshare driver settings. In Netmobiel, in particular in the frontend, are the user interfaces for the two roles combined in a single application. 

During the registration of a new user, the family name, given name and email address are copied from the token issued by Keycloak.

## Reviews and Compliments

Netmobiel supports a review and compliment mechanism. As Netmobiel is targeted at rural area with small communities, this mechanism is biased in positive direction. The system wants the user to given compliments, no negative things as a quarrel is easily started. The review text is free, there is no check on the content.

![Profile Review Compliment Class Diagram](Profile-Review-Compliment-Class-Diagram.png)

## Delegation
Netmobiel supports delegation, where someone can ask an informal carer to do the planning and searching for that person. The user who wants to become a carer of someone should first ask the Netmobiel administrator to assign the role `delegate`. The carer can then search for the profile or create a new profile for his client  and start the process to become a delegate. This process involves obtaining an activation code from the prospected carer client, the delegator.

![Profile Delegation Class Diagram](Profile-Delegation-Class-Diagram.png)

## Surveys
To research purposes, a feature has been added to redirect users to an external survey provider. On startup of the frontend application, it will ask the backend for any survey to take. If the proper conditions are met, the response will be non-empty. The frontewnd will then perform a redirect to the survey provider. Currently only [Qualtrics](https://www.qualtrics.com) is supported.

![Profile Survey Class Diagram](Profile-Survey-Class-Diagram.png)

## Logging of User Sessions
To investigate the usage of the application a simple DIY session logging has been added. It is simple and it's doing the job.  

The abbreviation CTA stands for call to action.

![Profile Session Log Class Diagram](Profile-Session-Log-Class-Diagram.png)


# Realtime

Realtime for *resesok-otp* in Skanetrafiken right is consumed by subscription to Azure Service Bus topics [[1]](#service-bus). The input format are XML SIRI messages [[2]](#siri).

The SIRI messages are sent to the corresponding topics for SX/ET messages. Each instance of *resesok-otp* in the Kubernetes cluster will create a subscription. When *resesok-otp*
is stopped then the corresponding subscription will be deleted from the Azure Service Bus.

![Azure Service Bus topic](/.images/service-bus-topic.png)

There is a common code base for all topic consumers (ET/SX and potentially in the future VM), this is the abstract class ``AbstractAzureSiriUpdater``. 
Each Azure Service Bus consumer and topic should extend this abstract class. Now there are two implementations, ``SiriAzureEtUpdater`` and ``SiriAzureSxUpdater``.

The subscription when created is set to be deleted if idle more then one day, so if e.g. *resesok-otp* is very abruptly shutdown, no dangling subscriptions will exist for more then
at most one day.

# Startup messages
When *resesok-otp* starts up it needs to get some SX/ET messages that have already been removed from the topics. 

TODO: Update this after implementation **This part of the documentation is a requirement that has not yet been well defined.**

We will have an end-point to get relevant messages SX and another ET that we will read in and inject into the Graph database, very similar to how EnTur has done. 
When the messages all have been consumed we should tell OTP that we are ready to start accepting requests (Kubernetes), again as EnTur. 

**All dates and timestamps are ISO-8601**

![ISO-8601](/.images/iso_8601.png)

# Bibliography

<a id="service-bus">[1]</a>
Azure Service Bus Messaging documentation
- https://docs.microsoft.com/en-us/azure/service-bus-messaging

<a id="siri">[2]</a>
Norweigian SIRI profile
- https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370420/Norwegian+SIRI+profile


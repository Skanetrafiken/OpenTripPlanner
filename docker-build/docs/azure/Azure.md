![Azure](/.images/msazure_logo.png)
# Introduction

The OpenTripPlanner for Skanetrafiken is hosted in Azure cloud. All internal tools are if possible also in the cloud. This documentation aims to give some insight to the Azure solution, but limited as much as
possible to *resesok-otp* not the platform in its totality.

# Azure Pipeline

TODO: Explain which files and the process (in a nutshell)

# Azure infrastructure
The Azure infrastructure is defined mainly through Terraform scripts and the Devops have some ownership of this. The *resesok-otp* Devops team has full ownership 
of [Kubernetes setup](/infra/Infra)

# Azure Storage accounts
We have one storage account for each environment, in each account we have two containers, *resesok-graph* and *resesok-netex*, that *resesok-otp* needs. These are used to get a database or NeTEx files to create
a new Graph database. [For more details see Infra](/infra/Infra).

The authorization is done with Kubernetes and AD identity defined in Terraform scripts on the *Infra* project.

# Azure Service Bus
We have an Azure Service Bus in our infrastructure [[1]](#service-bus). 
The purpose of this service bus is to publish and consume messages from topics and queues. At this moment there is a .NET project, *ROI*, that consumes realtime messages from *PubSub* and transforms them
to SIRI messages [[2]](#siri). These messegas are published on the Azure Service Bus on various topics. These messages are consumed in *resesok-otp*.

Permissions to receive and to create subscriptions are managed through Kubernetes AD. The credentials are created in Terraform in the *Infra* project. To test locally in
development the programmer can get a key from the Azure Service Bus on Azure services interface. The key is found in *Shared access policies*.

## Temporary topic
For development or debug purpose the topics can be difficult to use because the traffic is very high, specially for ET. The devoloper can in Azure Service Bus in development environment
create easily a new topic, e.g. *siriet-development* and configure it's local *resesok-otp* to subscribe to this topic. At this moment no realtime traffic will be received and
the programmer can create a SIRI XML message and send manually on this topic.

To send the message is trivial
1. In Azure services go to the correct Azure Service Bus
1. Goto *Topics* and select the desired topic
1. Select *Service Bus Explorer*
1. Set *Content Type* to *Application/Xml*
1. Put the SIRI XML message in the input field for the message.
1. Press *Send* and the message is broadcast to all subscriptions to the topic.

## Realtime
The realtime, i.e. the subscription to Azure Service Bus topics in OpenTripPlanner are developed and configured in the project code. [Further reading in Realtime documentation.](/development/Realtime.md).

When *resesok-otp* starts up and it has created a subsription to a topic it will start receiving realtime SIRI messages. Note well it only receives messages that are sent after
the subscription has been created.

# Bibliography

<a id="service-bus">[1]</a>
Azure Service Bus Messaging documentation
- https://docs.microsoft.com/en-us/azure/service-bus-messaging

<a id="siri">[2]</a>
Norweigian SIRI profile
- https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370420/Norwegian+SIRI+profile


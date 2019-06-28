#lang pollen

◊h1{Account management}

The main components of this system and their behaviour of the will be described in this document

We start with ◊em{firms}. They are our target clients.

◊h2{Firms}

There will be a onboarding process to add a new firm to our system.
The onboarding process will capture the relevant information about the firm.

◊h3{Basic Information}

◊ol{
◊li{Firm Name}
◊li{email}
◊li{Phone number}}

Before proceding, the above things should be verified as necessary.

◊h3{Portfolio}

Next, we move on to creating portfolio for the firm.
A portfolio is the showcase of the firm's existing work.
It may contain one or many ◊em{projects.}

A ◊em{project} refers to work done for a customer.
This may be past work, current work or any work that is to be done in the future.
This data can be used showcase the past projects, reflect and take action on
the ongoing project. Plan and manage multiple projects.
In other words, ◊em{project} is a central idea for us. More info on this will
be provided in the next section.

Coming back to the onboarding, in the next steps they should be able to create a showcase
of their past projects by adding projects and their data (images / 3d model etc)

◊h3{Invitations to team}

They should be able to invite other people to their workspace via email/phone.
Here, we can have following permission scheme:

◊ol{
◊li{Access to all projects}
◊li{Access only when added to project}}

We will discuss more about permissions in a later chapter.

◊h2{Designers et. al.}

A firm is made of designers as well as other people who help with the business. For us, this
is about access control of the actions to be performed on projects owned by the firm.

They can only be added by a firm by iniviting via email/phone.

◊h2{Customers}

They are the people for whom the project is being worked on.
Firm / Designers from their dashboard should be able to initiate a new project and add the
customer from there.

The customer will receive an email with the link to access the dashboard.

◊h2{Projects}

This is the central resource on which the people will work.
Some of the metadata for this are as following:

◊ol{
◊li{Project Name}
◊li{Start Date}
◊li{End Date}
◊li{Budget}
◊li{Images}
◊li{3d models}
◊li{Breakdown of components charged for}}

A typical breakdown of component could be on a per room basis.
In that case, a component should hold the following information

◊ol{
◊li{Label ◊i{(room name here)}}
◊li{Name of component}
◊li{Status ◊i{(Discussed / Ordered / Installed)}}
◊li{Dimensions}
◊li{Price}}

Note here, that we also need to be able to capture the date on which the status of a
component changed (could be done via additional columns or whatever)



# DM Seer Infra

A project to manage infrastructure for the DM Seer applications (frontend and backend).

## Setup

Make sure Pulumi (https://pulumi.com) is installed and set up for your AWS region and credentials.

## Create the Pulumi Dev Stack

Change to dev stack, if needed
`pulumi stack select dev`

Set aws region
`pulumi config set aws:region ca-central-1`

Create your stack
`pulumi up`


## Destroy the Stack
To save money 🤣

`pulumi destroy`

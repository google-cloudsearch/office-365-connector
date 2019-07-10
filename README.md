# Google Cloud Search Office 365 Identity Connector

The Google Cloud Search Office 365 Identity Connector enables synchronizing Office 365 principals
with the Google Cloud Identity service. This connector is intended to be used with the
[Google Cloud Search SharePoint Online Connector](https://developers.google.com/cloud-search/docs/guides/sharepoint-online-connector).

## Build instructions

1. Build the connector

   a. Clone the connector repository from GitHub:
      ```
      git clone https://github.com/google-cloudsearch/office-365-connector.git
      cd office-365-connector
      ```

   b. Checkout the desired version of the connector and build the ZIP file:
      ```
      git checkout tags/v1-0.0.5
      mvn package
      ```
      (To skip the tests when building the connector, use `mvn package -DskipTests`)

For further information on configuration and deployment of this connector, see
[Office 365 Identity Connector](https://developers.google.com/cloud-search/docs/guides/sharepoint-online-connector#configure-o365-identity).

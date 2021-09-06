# Using the Web Application Template and Tutorial

A Web Application, or WebApp, is a UI application that has specialized or application-specific
functions that are better-suited to the server-side than the browser-based UI, data processing, or control system Assemblies, HCDs, or Sequencers. 

TMT doesn't have many applications like this. It requires a user interface that is doing more than controlling hardware or sequencing. 
Server-side functionality may be computationally intensive functions, or it may need to use the CSW Database Service with specialized queries/results as examples.
At this time, only the APS has been identified as a user interface that needs the Web Application approach.

A Web Application has a frontend and a backend.  The Web Application frontend uses the same tools and libraries as the Gateway-only UI. 
The WebApp UI will typically use the UIAG as well as its own backend service that implements the application-specific routes that are called by the UI. 

### Web Application Template and Tutorial

A template Giter8 project has been provided that should be used when creating a new Web Application project.
The template provides starter code for the frontend UI and the backend service.
Please follow the instructions in the README to get started with the template. The repository is located  @link:[here](https://github.com/tmtsoftware/esw-web-app-template.g8).

There is also a tutorial and example that builds from the template @link:[here](https://tmtsoftware.github.io/esw-web-app-example/0.1.0-SNAPSHOT/index.html) { open=new }.
The tutorial shows how to create a project using the template and go through the steps needed to create a web application backend that works with the frontend.

Twitter-Model
=============

This project deals with modelling the Twitter API. 

Part 1 (4-1) is implemented using akka actors. Aim of the first part was to stress test the server and see how much can a pure
akka server withstand the client requests.
Client contacts the server for tweeting, getting its timeline, getting its followers etc.

Part 2 (4-2) is implemented using spray with REST-API on akka. Aim of the second part was to build a web server that accepts http requests and responds with http responses. The server is built using the spray-route and client with the spray-client API.
Client contacts the server by making GET and POST requests. 

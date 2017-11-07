#!/bin/bash

javac -d bin -sourcepath src -cp lib/gson-2.6.2.jar src/Server.java

java -cp ./bin:./lib/gson-2.6.2.jar Server 43

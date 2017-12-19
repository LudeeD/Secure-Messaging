#!/bin/bash

javac -d bin -sourcepath src -cp lib/gson-2.6.2.jar src/Server.java

if [ $? -ne 0 ]; then
    echo -e "\e[31mErrors, pls Reverify Code\e[0m"
    exit 1
fi

java -cp ./bin:./lib/gson-2.6.2.jar Server 43

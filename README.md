# DistrAlgoSimulation
Distributed Computing Algorithms simulation using AKKA (Scala) toolkit

This project explores various renowned wave algorithms within the realm of distributed computing. It is developed using the Akka typed framework, which leverages the Actor model to create a distributed computing environment. This project utilized graphs generated from [NetGraphSim](https://github.com/0x1DOCD00D/NetGameSim) (it can be used to generate large scale random graphs to run simulations on top)

{

- Create a distributed network simulation using Akka(Scala)
- How to use Akka-typed with Scala 3 (for Beginners also) 
- Implement Distributed Wave Algotithms including: Tarry's, DFS-Wave, Optimized-DFS Wave, Awerbuch's, Cidon's, Tree and Echo Wave Algorithms <br>

}

## Simple Installation  Guide

### Prerequisites
+ Java Development Kit (JDK) 11
+ Scala 3.2.2
+ SBT (Scala Build Tool) 1.8.3


### application.Conf
Inside base-directory/src/main/resources/, you can find the application.conf file. It contains:
-  ngsGraphDir -> directory of the .ngs Graph binary file
-  ngsGraphFileName -> name of the .ngs Graph binary file
(binary files that store graph info carries .ngs extension in ([NetGraphSim](https://github.com/0x1DOCD00D/NetGameSim)))

### Steps:
1. clone this project
2. from the base directory of the project, go to the terminal and run>> sbt clean compile
3. Then run command >> sbt run

(You can also run the application from IntelliJ or some other related IDE)

### Additional Notes
Make sure to update the build.sbt file if you need to add dependencies or make any configuration changes specific to your project.<br>
Refer to the official documentation for Scala, Akka, and SBT for more detailed information and troubleshooting tips.



## Usage
The input directory inside the base directory includes some .ngs graph files that can be utilized to run and view this distributed wave algorithm simulations. To utilize one, set the appropriate dir and filename in the application.conf file.

1. Once the application is running, it will load the graph data, and prompt for user input as follows,<br>

Welcome to AkkaWaveSim
Select one from the following to initiate a wave algorithm simulation:
1. Tarry's Algo[1]              2. DFS Algo[2]              3. Optimized DFS Algo[3]        
4. Awerbuch's Algo[4]           5. Cidon's Algo[5]
6. Tree Algo[6]                 7. Echo Algo[7]<br>
Enter choice[1-7]:<br>

- A simulation based on the graph and your choice of algorithm will be run. All the important information and messages exchanges will be logged using Akka context's logger. Log files will be available in the log folder inside the base director

- Once a simulation is complete, the prompt will reappear asking for algorithm choice, enabling running simulations in a loop.

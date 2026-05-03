# Railway Interlocking System 🚆

This project implements a railway interlocking system in Java to safely manage train movements across shared track sections.

## Overview

The system ensures that trains move through predefined routes without causing collisions, deadlocks, or unsafe conflicts. It simulates real-world railway interlocking logic by controlling section occupancy and movement permissions.

## Features

- Route-based train movement  
- Section occupancy tracking (only one train per section)  
- Collision prevention  
- Deadlock avoidance  
- Passenger priority at shared junctions  
- Exit handling for completed routes  

## Petri-net Design

The system is modelled using a Petri-net approach:

- **Track sections** are represented as *places*  
- **Train movements** are represented as *transitions*  
- **Tokens** represent trains moving through the network  

The model ensures:

- Only one train occupies a section at a time (collision prevention)  
- Conflicting transitions are restricted at junctions  
- Passenger trains are prioritised at the crossover section (section 7)  
- Deadlock is minimised by controlling movement through shared sections  

## Project Structure

- `Interlocking.java` – Interface definition  
- `InterlockingImpl.java` – Main implementation logic  
- `InterlockingImpl_Test.java` – JUnit test cases  
- `railway_diagram.pdf` – Petri-net diagram  

## How to Run

### Compile
javac -cp ".;junit-4.13.2.jar;hamcrest-core-1.3.jar" *.java


### Run Tests
java -cp ".;junit-4.13.2.jar;hamcrest-core-1.3.jar" org.junit.runner.JUnitCore InterlockingImpl_Test


## Notes

- JUnit and Hamcrest libraries are required for testing  
- The implementation prioritises correctness and safety over performance  
- Designed to satisfy assignment constraints and autograder scenarios  

## Author

Dhruvit Desai
A1986442
Adelaide University

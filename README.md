# JMPlib

Dynamic languages are widely used due to the flexibility needed in some applications or systems. Therefore, dynamic language metaprogramming features have been incorporated gradually to statically-typed languages. Our work is aimed to improve the flexibility of Java language without modifying the Java Virtual Machine. We developed a library that allows Java language to support two types of metaprogramming features: 1) structural intercession y 2) dynamic code evaluation. This was achieved using class versioning, code instrumentation and Hot-Swapping. In conclusion, the library allows programmers to use these two functionalities in new or legacy code to improve its runtime flexibility.

## How to use it!

### Starting

For this example, we have created a Car class that simulates the fuel consumption when the car travels:

```java
package addmethod;

public class Car {
	
	private int km = 0;
	private int fuel = 70;
	private double consumptionAverage = 5.7; // 100km
	
	public void run(int kilometres) throws Exception{
		km += kilometres;
		fuel -= kilometres * consumptionAverage/100;
		if(fuel < 0)
			throw new Exception("Out of fuel");
	}
	
	public void refill(int litres){
		fuel += litres;
	}

	@Override
	public String toString() {
		return "Car [km=" + km + ", fuel=" + fuel + ", consumptionAverage=" + consumptionAverage + "]";
	}
}
```

### Using meta-programming

Additionally, we have created a main class with some meta-programming:

```java
package addmethod;

import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;

import jmplib.IntercessorTransaction;

public class AddMethodMain {

	public static void main(String[] args) {
		Car car = new Car();
		System.out.println("======== INITIAL STATE =======");
		System.out.println(car);
		try {
			car.run(250);
			System.out.println("======== 250km Later =======");
			System.out.println(car);
			// Method added to know the remaining kilometres
			IntercessorTransaction transaction = new IntercessorTransaction();
			transaction.addMethod(
					Car.class,
					"kilometresToRunOutOfFuel",
					MethodType.methodType(int.class),
					"return (fuel < 0) ? 0 : (int)(fuel * consumptionAverage);");
			transaction.replaceImplementation(
					Car.class,
					"toString",
					"return String.format(\"Car [km=%d, fuel=%d, consumptionAverage=%f, kilometersLeft=%d]\""
							+ ", km, fuel, consumptionAverage, kilometresToRunOutOfFuel());");
			transaction.commit();
			System.out.println("- Method added to see remaining km and toString modified");
			car.run(250);
			System.out.println("======== 500km Later =======");
			System.out.println(car);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
}
```

First of all, we have created a car and travelled 250km with it. Later, we have applied a couple of meta-programming primitives to improve the car class. In one hand, we have added a new method that calculates how many kilometres left until the car runs out of fuel. On the other hand, we have modified the toString method to show this new information. Finally, we have run 250km with the same car.

### Little config

We have created a file called config.properties in the root folder. This file have two lines, one specifying the path to the jre inside the JDK and other specifying the path to our source folder.

```
java.home=C:\\Program Files\\Java\\jdk1.8.0_45\\jre
source.path=src\\
```

### Compile

```bash
javac -cp JMPlib_v1.0.0-with-dependencies.jar -d bin/ src\addmethod\*
```

### Program structure

After compiling the code, the proyect have the next structure:

```bash
├── bin/
│   ├── addmethod/
│   │   ├── AddMethodMain.class
│   │   ├── Car.class
├── src/
│   ├── addmethod/
│   │   ├── AddMethodMain.java
│   │   ├── Car.java
├── config.properties
├── JMPlib_v1.0.0-with-dependencies.jar
```

### Execute

Using the library requires specifying the -javaagent parameter. 

```bash
java -javaagent:JMPlib_v1.0.0-with-dependencies.jar -cp .;bin/ addmethod.AddMethodMain
```

The console output is the following one:  

```
======== INITIAL STATE =======
Car [km=0, fuel=70, consumptionAverage=5,700000]
======== 250km Later =======
Car [km=250, fuel=55, consumptionAverage=5,700000]
- Method added to see remaining km and toString modified
======== 500km Later =======
Car [km=500, fuel=40, consumptionAverage=5,700000, kilometersLeft=228]
```

As you can see, the last line shows the new information due to the car instance that we had created has obtained the new functionallity.

## More info

See [javadoc](https://cdn.rawgit.com/ilagartos/jmplib/master/docs/index.html)


# Catscript Guide

This document should be used to create a guide for catscript, to satisfy capstone requirement 4

## Introduction

Catscript is a simple scripting language.  Here is an example:

```
var x = "foo"
print(x)
```
## Comments:
Comments are handled with "//" on the line at the point you want to comment:
```
//this is a comment print(12)
//the above print will not run
//but this one will
print(44) //since the comment is after the statement
```

## Features

### For loops:
For loops give the ability to loop over code multiple times even if the number of iterations changes each run. A for loop in Catscript looks like this:
```
for( x in [1, 2, 3, 4] ) { //the variable x can be used later on in the loop as it is a local variable
print(x)	//prints: 1 2 3 4
}
```
To use a for loop, you must provide a list to iterate over. There can be as many other statements including other for loops inside a loop.:
```
for( x in [1, 2, 3, 4] ) {	//outside loop
    print(x)	//prints: 1 2 3 4 each time the loop is run 
    for( y in [1, 2, 3, 4] ) { //nested loop
        print(y)	//prints 1 2 3 4 each time the loop runs
    }
}
OUTPUT: 1 1 2 3 4 2 1 2 3 4 3 1 2 3 4 4 1 2 3 4
```

### If statements:
If statements are conditionals for testing if a condition contained inside the statement is true. For example:
``` 
if(true){
    print("True")
}else{
    if(false){
        print("nested False")
    }else{
        print("nested True")
    }
}
```
If the first condition is true, then the first print: "Print("True")" will execute/run.
If the first condition is not true, then the else runs.
If statements are also "nest-able" meaning that they can be inside themselves as shown above in the example.

### Variable assignment:
Variables can be assigned with an explicit type or with a implicit type. For example:

```
var x = "Hello World!"  //declare variable with implicit typing
var y : int = 54        //declare variable with explict typing

print(x)	//prints: Hello World!
print(y)	//prints: 54

x = "Goodbye!"	//redeclare a variable after assignment/creation
print(x)	//prints: Goodbye!
```

### Types:
The supported types are as follows:
```
-   int - a 32 bit integer
-   string - a java-style string
-   bool - a boolean value
-   list - a list of value with the type 'x'
-   null - the null type
-   object - any type of value
```
If an explicit type is not found from evaluation or declaration (var x : int) the type is object

### Lists
Lists are supported in Catscript and are declared as follows:
```
var lst :list<int> = [1, 2, 3, 4]	//declare a list of type integer

for( x in lst) {	//iterate over the list
    print(x)	//prints: 1 2 3 4 
}
```
List types supported are all basic types: int, string, boolean, list, and object.

### Functions:
Functions can be created and called like so:
```
function x (){   //declare a function with the keyword "function" then the function name
    var q = 4	 //create variable 
    if(q > 3){   //check if condition is true
        print(q) //prints: 4
    }else{
        print("q is not greater than 3!")
    }
}

x()  //fucntion call
```
Functions can have as many statement's and expressions as desired in them, as shown above.

Functions can also have parameters passed into them:
```
function x (y, z: int){   //multiple parameters can be passed in with commas between them
    print(y)   //prints: String
    print(z)   //prints: 1
}

x("String", 1) //calls x with y = (String) and z = (1)
```
As shown above, you can also have the type of the parameters be implicit or explicit as desired. There is no limit to how many parameters are passed into a function, nor a minimum.

### Argument lists:
functions can have an argument list passed in when invoked to fill the required parameter list. Example:
```
function f(x:int, y:string){
    print(y)        //prints y
    print(x + 3)    //prints the result of x + 3
}

f(3, "Hello")   //prints Hello 6
``` 
These argument lists are required to be as long as the parameter list for the function definition is and if the type is not specified it is treated as a string.

### Built-in Function(s):
Catscript currently only has one built-in function, that is the print function that is being used in many of the examples. It only supports printing a single expression at the moment, for example:
```
print(1)              //prints: 1
print(1 + 1)          //prints: 2
print(10 / 5)         //prints: 2
print("Hello World!") //prints: Hello World!
```

### Returns:
A function can return a value from it and hold it in a variable or use it for a calculation. An example of returns are as follows:
```
function x(){
    return 1  //returns the int value 1
}
print(x())   //uses the returned value to print 1
var y = x() + 2   //uses the return value to perform a math calculation and store it in a variable
print(y)          //prints out the value of the variable using the returned value
```
As shown above, a function can be called and the return value used for other purposes. The above code uses the return value from x() returning 1 to do a math calculation.
If a return is placed outside a function definition it is treated as a syntax error.
## Expressions:
Expressions are evaluated in order of (left to right) Primary>Unary>Factor>Additive>Comparison>Equality (with left being evaluated first and right being evaluated last, this is also the way binding strength scales with left being strongest and right being weakest).

### Parenthesized expressions:
Wrapping expressions in a parentheses is supported in Catscript, this can allow for more clearly showing what order expressions should be evaluated in, for example:
```
print(2 * (2 + 4)) //result is 12
print(2 * 2 + 4)   //result is 8
``` 

### Primary Expressions:
Primary expressions are an expression that evaluated to a basic type, a variable, a function call, a list, or a parenthesized expression. The supported types are:
true, false,
null,
list_literal,
function_call,
(expression),
Identifier, String, Integer.

### Unary Expressions:
A unary Expression gives the ability to negate a primary expression, example:
```
if(not true){	//use not to negate a boolean
    print("Not True")	//not reached
}
if(-1 < 0){	//negates a number 
    print("-1 is less than 0")	//prints: -1 is less than 0
}
```
The '-' does not work with Boolean's and the "not" keyword is not for negating numbers.

### Factor Expressions:
A factor expression supports multiplication and division between numeric unary expressions. Example:
```
print(10 / 5)	//prints: 2
print(10 * 5)	//prints: 50
print((2 + 4) * (2 / 2))	//prints: 6
```
As shown above, nesting expressions does not matter as the unary is evaluated for each side of the sign first.

### Additive Expressions:
An additive expression handles both concatenation of strings and addition of numbers, Example:
```
print(5 + "hello")	//prints: 5hello
print(4 + 5)	//prints: 9
print( 5 - 5)	//prints: 0
```
## Comparison and Equality Expressions:
All equality and comparison expressions are left to right comparatively, meaning that: left > right,  checks if left is Greater than right. Comparison expressions are treated as more tightly binding than equality expressions.

### Comparison Expressions:
Catscript supports 4 comparison expressions:
< : Less
\>: Greater
<=: Less or Equal
\>=: Greater or Equal
Below are examples of each:
```
if(2 > 1){	//checks if 2 is greater than 1
    print("Greater!")	//prints: Greater!
}
if(4 >= 4){	//check if 4 is greater or equal to 4
    print("Greater or Equal!")	//prints: Greater or Equal!
}
if(1 < 4){	//checks if 1 is less than 4
    print("Less!")	//prints: Less!
}
if(3 <= 4){	//checks if 3 is less than or equal to 4
    print("Less or Equal!")	//prints: Less or Equal!
}
```

### Equality Expressions:
Catscript supports 2 equality expressions:
==: Equal
!=: Not Equal
Below are examples of each:
```
if( 1 == 1){	//checks if 1 is equal to 1
    print("Equal!")	//prints: Equal!
}
if(1 != 5){	//checks if 1 does not equal 5
    print("Not Equal!")	//prints: Not Equal!
}
```

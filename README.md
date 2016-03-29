## Interpreter Warp 10

This plugin can be used to interpret some Warp10 code. It will send an HTTP post to Warp10 with the current paragraph strings.
The result provided is a JSON string. 
To get started configure the Warp10 interpreter via interpreters adding the url of the Warp10 backend as parameter.
```
name:                 value:
warp10.url           http://localhost:8086/api/v0
```

## NaN and INFINITY case
The JSON library parse NaN and INFINITY as strings.

## Share some data with the others interpreters

Data generated via Warp10 can be communicated to other plugins via the resource pool of the interpreter Context.
If the first element on top of the stack in the Warp10 result is a Map, then all the elements contained in this Map are saved.
To import element in Warp10 add a new line right after %warpscript:
```
//import var1 var2
```
All variables indicated here will then be loaded in Warpscript. 
To delete elements, in the result map add NULL as value. The key will then be deleted.
Those elements can then be loaded in an other interpreter. For example in spark:

```
// Use z.get to load a variable name "sample" in resource pool
val a = z.get("sample")
//  Use put to add a new variable
z.put("spark", "test")
```

## Use EXPORT
The EXPORT function available in Warpscript assure a Map as first element in top of stack.
Define here a list of variable and their last instances will be saved.

With the following example both key sample and spark will be inside a Map on top and will be saved in resource pool.
```
%warpscript
// import spark
[ 'sample' 'spark' ] EXPORT
'Warpscript' 'sample' STORE
```

## Set-up 

Compile the interpreter with maven

```
mvn clean package
```

Create a directory warp10 in interpreters

Then copy the Jar with dependencies inside

Stop and restart zeppelin
## Interpreter Warp 10

This plugin can be used to interpret WarpScript instruction. It will send an HTTP post to Warp10 with the current paragraph lines.
The result provided is a JSON string. 
To get started configure the Warp10 interpreter, adding the url of the Warp10 backend as parameter.
```
name:                 value:
warp10.url           http://localhost:8086/api/v0
```

## NaN and INFINITY case
The JSON library parse NaN and INFINITY as strings.

## Share some data with the others interpreters

Data generated via Warp10 can be communicated to other plugins via the resource pool of the interpreter Context.
If the first element on top of the result stack in Warp10 is a Map, then all the elements contained in this Map are saved.

To import element in Warp10 add a new line right after '%warpscript'.
```
//import var1 var2
```
All variables indicated here will then be loaded in WarpScript. 
To delete elements, in the result map add NULL as value. The key will then be deleted.
Those elements can then be loaded in an other interpreter (example with Spark).

```
%spark
// Use z.get to load a variable name "sample" in resource pool
val a = z.get("sample")
//  Use put to add a new variable
z.put("spark", "test")
```

## Use EXPORT
The EXPORT function available in WarpScript assure a Map as first element in top of stack.
Define here a list of variable and the last value of those variables will be saved.

With the following example both key 'sample' and 'spark' will be inside a Map on top of the Warp10 stack and will be saved in resource pool.
```
%warpscript
// import spark
[ 'sample' 'spark' ] EXPORT
'WarpScript' 'sample' STORE
```

## Set-up 

Compile the interpreter with maven.

```
mvn clean package
```

Create a directory warp10 in folder interpreter.

Then copy the Jar with dependencies inside.

Add in file conf/zeppelin-site.xml, for the property zeppelin.interpreters, this interpreter: 

```
<property>
<name>zeppelin.interpreters</name>
<value>org.apache.zeppelin.spark.SparkInterpreter,...,org.apache.zeppelin.warp10.Warp10Interpreter</value>
</property>
```

Stop and restart Zeppelin.

## Deploy

To deploy this jar with scp add the follwing property file etc/config/local.properties containing: 

```
sshUrl=scp://user@my.domain:/path/to
```

Excute target deploy

```
mvn deploy -Drat.skip=true
```

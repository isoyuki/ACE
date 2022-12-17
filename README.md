## Requirements

- Java 11
- Maven
- Redis

## Build

`cd` into the root of the project directory and run the following command to compile and output the Jar file:

```bash
cd <folder name>
mvn package
```

The output should be in the `target` folder.

## Usage

**Generation**

The document should follow the structure below:
  ```
  id1 keyword1,keyword2,...
  id2 keyword3,keyword4,...
  ```

Run the following command to create new encrypted database

```bash
  cd target
  java -cp "libs/*:ACE-1.0-SNAPSHOT.jar" org.ace.core.generator.Generator update /path/to/document.txt new
```

> Ensure that Redis is running before executing the above command


**Update**

To add new records into the database, run the following command:

```bash
  cd target
  java -cp "libs/*:ACE-1.0-SNAPSHOT.jar" org.ace.core.generator.Generator update /path/to/new_document.txt
```

**Search**

To search for a keyword, run the following command:

```bash
  cd target
  java -cp "libs/*:ACE-1.0-SNAPSHOT.jar" org.ace.core.client.Client keyword_to_search
```

**Delete**

To delete an ID, run the the following command:

```bash
  cd target
  java -cp "libs/*:ACE-1.0-SNAPSHOT.jar" org.ace.core.generator.Generator delete ID_to_delete
```
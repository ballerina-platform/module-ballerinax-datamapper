# Ballerina Data Mapper Extension
A compiler extension to extract an abstract representation of Ballerina connector actions and their associated types. When compiling a 
Ballerina connector project this compiler extension gets executed. The extension generates two types of files by default. Note that 
these two types of files get automatically extracted from connectors hence developer does not need to write them.

##Functions File
These files end with a file name ``<CLIENT_NAME>_functions.json`` where the `CLIENT_NAME` corresponds to the connector client name.
When there are ``n`` clients exist in a connector, ``n``  number of functions files get created. Function files get created only if there 
are client objects with remote function calls. These files can be used to identify the list of connector actions available in a 
particular connector as well as to identify what types of records are used as input parameters and return types.

##Schema File
These files end with a file name ``<RECORD_NAME>_schema.json`` where the `RECORD_NAME` corresponds to a record type defined
within the connector module. In order for a schema file to be generated for a record type defined within a connector module, the
record has to be one of the following,
 
 1. An input parameter or a return type from a connector action.
 2. A record type referred by a record which falls into the category (1).
 
  In addition to the above two types of JSON file categories which gets automatically extracted from a connector by default, 
  the connector developer can also specify sample data in files having file names `<RECORD_NAME>_data.json`.
  
  ##Data File
  Data files have to follow the structure given below.
  
```
{
    "<ORG_NAME>/<MODULE_NAME>:<VERSION_NUMBER>:<RECORD_NAME>": [
        <SAMPLE_JSON_INSTANCE_1>,
        <SAMPLE_JSON_INSTANCE_2>,
        <SAMPLE_JSON_INSTANCE_3>,
        ...,
        <SAMPLE_JSON_INSTANCE_N>
    ]
}
```
The `<SAMPLE_JSON_INSTANCE_i>` should have a structure as follows where the JSON 
instance has `M` number of attributes,

```
{
    "<ATTRIBUTE_NAME_1>" : "<ATTRIBUTE_VALUE_1>",
    "<ATTRIBUTE_NAME_2>" : "<ATTRIBUTE_VALUE_2>",
    "<ATTRIBUTE_NAME_3>" : "<ATTRIBUTE_VALUE_3>",
    ...,
    "<ATTRIBUTE_NAME_M>" : "<ATTRIBUTE_VALUE_M>"
}
```

An `<ATTRIBUTE_VALUE_j>` can be a simple literal (string/numeric value), 
`<SAMPLE_JSON_INSTANCE_j>`, an array, or `null`.

Unlike functions files and schema files which automatically gets extracted, 
from the Ballerina Abstract Syntax Tree (AST), data files are user specified JSON files. 
Hence the Data Mapper compiler extension conducts a validation of the content of the data JSON files.
The validation process checks whether the data json file's attribute names are equivalent to the 
attribute names used in their corresponding types. Furthermore, it checks whether the same number 
of attributes exists between the record type and its corresponding data JSON. In addition to these 
validations, the compiler extension also extracts sample data nested within a particular data JSON 
to separate top level data JSON files.

The extracted json files get written to the resources directory within the connector module's 
`src` folder. However, the functions and schema JSON files are not required to be committed to a
Github repository since they get automatically generated from the connector's Ballerina code. However, 
the data files need to be version controlled.

  
  
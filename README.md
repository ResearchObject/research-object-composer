# Research Object Composer

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

* [API documentation](https://researchobject.github.io/research-object-composer/api)
* [JavaDoc](https://researchobject.github.io/research-object-composer/javadoc)

## About

The Research Object Composer is a web service that facilitates the creation of Research Objects, 
constrained to a pre-defined profile.

It uses JSON as an intermediary format for populating the Research Objects, 
and uses JSON Schemas (with some modifications) as "Profiles" to validate their correctness/completeness. 

The Research Objects are serialized as zipped BagIt-ROs, and automatically deposited in a pre-configured repository.


## Installation

### Manual
```
./mvnw spring-boot:run
```

### Docker

To start your own instance using [Docker Compose](https://docs.docker.com/compose/)

```
docker-compose up -d
```

To run the [Jupyter Notebook](https://jupyter.org/) tutorial:

1. Download this git repository or the [introduction.ipynb](introduction.ipynb) file from GitHub
2. Install [Anaconda](https://docs.anaconda.com/anaconda/install/) (Python 3 variant)
3. In the Anaconda Prompt or Terminal, create a new Conda environment for the RO Composer and start Jupyter Notebook 
```
conda create -n rocomposer jupyter
conda activate rocomposer
jupyter notebook introduction.ipynb
```

## Usage

### Configuration

...

### Profiles
Profiles are specified as [JSON Schemas](https://json-schema.org/) in `public/schemas`, and are named `<name>.schema.json`.
Schemas are automatically discovered and turned into Research Object Profiles when the application boots, and are available at `/profiles/<name>`. 

Schemas with filenames prefixed with an underscore `_` are not automatically loaded as Research Object Profiles, 
but can still be referenced by other schemas (i.e. using `$ref`) and used in validation.

`_base.schema.json` contains several definitions that are understood natively by the Research Object Composer 
and should be referenced by any implementing schemas:
* `RemoteItem` - Metadata for a remote file, including its URL, file size, checksums etc.
* `Metadata` - Metadata for the Research Object itself, including title, description, authors etc.

They can be referenced like so:
  
```json
{
  "properties" : {
    "_metadata": {
      "$ref": "/schemas/_base.schema.json#/definitions/Metadata"
    }
  }
}  
```

##### $baggable
`$baggable` is a special schema keyword that tells the Research Object Composer which properties contain references to 
remote files, and where they should be included in the BagIt-RO. It is a simple map of `<property name> : <relative path in the bag>`.

For example the following schema:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema",
  "type" : "object",
  "$baggable": {
    "data" : "/"
  },
  "properties" : {
    "_metadata" : {
      "$ref": "/schemas/_base.schema.json#/definitions/Metadata"
    },
    "data": {
      "type": "array",
      "items": {
        "$ref": "/schemas/_base.schema.json#/definitions/RemoteItem"
      }
    }
  },
  "required": [
    "data"
  ]
}

```

...tells the Research Object Composer to bag anything found under `data` at the root of the bag (`/`).

##### _metadata
`_metadata` is not a schema keyword, but a recommended property of the Research Object JSON. It contains a minimal set of 
metadata about the Research Object itself, such as title, description, license, authors etc. 
It should reference `/schemas/_base.schema.json#/definitions/Metadata`.
 
The Research Object Composer will provide this metadata (and perform any necessary mapping) to the configured repository when depositing a Research Object. 

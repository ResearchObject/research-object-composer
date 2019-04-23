# Research Object Composer

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

* [API documentation](https://researchobject.github.io/research-object-composer/api)
* [JavaDoc](https://researchobject.github.io/research-object-composer/javadoc)

To start your own instance using [Docker Compose](https://docs.docker.com/compose/)

```
docker-compose up -d
```

To run the [Jupyter Notebook](https://jupyter.org/) tutorial:

1. Install [Anaconda](https://docs.anaconda.com/anaconda/install/) (Python 3 variant)
2. Set up [BioConda](https://bioconda.github.io/#install-conda) channels:
  ```
conda config --add channels defaults
conda config --add channels bioconda
conda config --add channels conda-forge
  ```
3. Create a new Conda environment for the RO Composer and start Jupyter Notebook
```
conda create -n rocomposer jupyter
conda activate rocomposer
pip install -r requirements.txt
conda activate rocomposer

```

# Research Object Composer

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

* [API documentation](https://researchobject.github.io/research-object-composer/api)
* [JavaDoc](https://researchobject.github.io/research-object-composer/javadoc)

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


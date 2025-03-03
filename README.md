# STARS experiments that analyze the effects of uncertainties on scenario coverage

This repository analyzes driving data recorded with the [Carla Simulator](https://carla.org/) using the [STARS 
framework](https://github.com/tudo-aqua/stars). The Carla data was recorded using the [stars-export-carla](https://github.com/tudo-aqua/stars-export-carla)
Repository. It analyzes the effects of uncertainties of predicates and their evaluation on scenario coverage.

Currently, the repository works by including the STARS-Framework as an included build (see `settings.gradle.kts`).
The repository has to be at the current state of the `owa-coverage` branch! Otherwise, necessary changes are not
available in this repository.

## Setup

The analysis requires the recorded data. To receive the data, there are two options:
1. Set `DOWNLOAD_EXPERIMENTS_DATA` in `experimentsConfiguration.kt` to `true`. This will automatically download and 
   unzip the necessary data.
2. Manually download the data. 
   1. Go to the [Zenodo artifact](https://zenodo.org/record/8131947) where the experiments data is stored
   2. Download the `stars-reproduction-source.zip`
   3. Place the Zip-File into the root folder of this project.

**Remark:** The downloaded data has a size of approximately 1.3GB. The downloaded zip-file will be extracted during 
the analysis. Make sure, that you have at least 3GB of free space.

## Running the Analysis

This project is a Gradle project with a shipped gradle wrapper. To execute the analysis simply execute:

- Linux/Mac: `./gradlew run`
- Windows: `./gradlew.bat run`

### Additional Arguments

The project provides the possibility of adjusting the analysis and its environment. The following arguments are 
provided:

- ``--help``: Returns a list of all available arguments.
- ``--input`` (String): Points to the directory of the input files that should be analyzed.
- ``--allEgo`` (Boolean): Sets, whether every vehicle should be treated as _ego_ and therefore increasing the analyzed 
data by the amount of available vehicles.
- ``--minSegmentTicks`` (Int): Sets the minimum amount of ticks that have to be present for a segment to be analyzed.
- ``--sorted`` (Boolean): Sets, whether the input files should be sorted be the `seed` in the files name.
- ``--dynamicFilter`` (String): A regex filter that is applied to all dynamic files effectively shrinking the analysis set.
- ``--staticFilter`` (String): A regex filter that is applied to all static files effectively shrinking the analysis set.
- ``--ignore`` (String, separated by ','): A string list filter that is applied to all TSC projections. Make sure to 
remove all unnecessary white spaces, as they propagate to the final ignore list.

Each argument has a default value to recreate the experiment data from the [linked paper](https://doi.org/10.1007/978-3-031-60698-4_15) 
in which the results of this repository are used.

Arguments can be passed to the analysis with the following command structure:  

- Linux/Mac: `./gradlew run --args="--allEgo --minSegmentTicks=20 --sorted --dynamicFilter='seed18' --staticFilter='Town01' --ignore='all,static'"`
- Windows: `./gradlew.bat run --args="--allEgo --minSegmentTicks=20 --sorted --dynamicFilter='seed18' --staticFilter='Town01' --ignore='all,static'"`

## Problem Solving

If you are running into `OutOfMemory` exceptions, you can add the following statement to the `application{}` block in 
your `build.gradle.kts` file to adjust the available heap size for the application:

``
applicationDefaultJvmArgs = listOf("-Xmx12g", "-Xms2g")
``

The example above will set the max heap size to 12GB (`-Xmx12g`) and the initial heap size to 2GB (`-Xms2g`).

## Analysis Results

After the analysis is finished you can find the results in the `analysis-result-logs` subfolder which will be 
created during the analysis.

For each execution of the analysis pipeline a subfolder with the start date and time ist created. In it, each metric 
of the analysis has its own subfolder. The analysis separates the results of each metric into different categories 
with different detail levels. 
- `*-severe.txt` lists all failed metric results
- `*-warning.txt` lists all warnings that occurred during analysis
- `*-info.txt` contains the summarized result of the metric
- `*-fine.txt` contains a more detailed result of the metric
- `*-finer.txt` contains all possible results of the metric
- `*-finest.txt` contains all possible results of the metric including meta information

## IntelliJ Analysis Helper

Intellij can (by default) only handle file up to 2.6MB. As the result file are in many cases larger than this 
threshold, you can increase the size of files that should be analyzed by IntelliJ. You have to restart IntelliJ
for the changes to take effect.

```
# custom IntelliJ IDEA properties (expand/override 'bin\idea.properties')


idea.max.intellisense.filesize=999999
```

#### (Optional) Git Hooks
If you want to use our proposed Git Hooks you can execute the following command:
```shell
git config --local core.hooksPath .githooks
```
# LogComp: A Log-Specific File Compressor
## Description

`LogComp` is a log-specific file compressor that helps popular, already efficient general-purpose file compressors (like `zip`, `7z` etc.) to achieve better compressions on log file specifically.

## Endpoints

`LogComp` is a web application developed in `Java 17.0.6`, using `Spring Boot 3.2.3` as the base framework for managing the embedded web server `Tomcat`, and `Maven` as the build automation and dependency management tool. It has 4 endpoints:

- `POST /compress` - Upload a decompressed log file for compression
- `GET /compress/download` - Download the compressed log file
- `POST /decompress` - Upload a compressed log file for decompression
- `GET /decompress/download` - Download the decompressed log file

> Note: `LogComp` is intended to be only used in a local environment, as it cannot process multiple requests at the same time. If a request is sent to the application while it is already processing, the application will response with `429 Too Many Requests` error HTTP status code.
## Adding a new log format

In order for a new log format to be supported for compression/decompression by the application, 3 lines are required to be added in the `log-format.txt` configuration file:

- An **unique identifier**
- A **regex statement** that matches the log lines; *capturing groups* should be used for the log fields
- The regex statement above, but the *capturing groups* are replaced with the **type of repetitiveness identifiers**

There are 5 types of repetitiveness for a field:

- `rep` - for sequences that are likely to repeat over many consecutive lines
- `num` - for numerical sequences (may contain by default `:,.`) that are likely to repeat over many consecutive lines, but with a slight variation
- `numf` - variation of `num` that allows zeros before the numerical sequences in order to keep the same number of characters throughout the field
- `dict` - for sequences that are likely to repeat
- `msg` - reserved for the message field, which should be always the last


We take the example the following log line:

```
2015-07-29 19:26:21,044 - WARN  [RecvWorker:QuorumManager@765] - Interrupting SendWorker
```

We have to add in `log-format.txt` the following 3 lines

```
zookeeper  
^(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2},\d{3}) - (\w+ *) \[(.+):(.+)@(\d{1,4})\] - (.+)$  
rep num - dict [dict:dict@num] - msg
```

> Note: The first log line in the log file has to be valid.

> Note: The performance of `LogComp` is highly dependent on the sequences that appear in the fields. A different combination of the type of repetitiveness identifiers can produce better compression for a particular log file. For example, for an application that is *single-threaded*, `rep` would be more suitable than `dict` if the log lines contain a *thread identifier field* (ex. `Thread-1`).

## Compression steps

- The original log file desired to be compressed is sent through `POST /compress` endpoint. At this step, `LogComp` is generating the intermediate file
- The intermediate file generated by `LogComp` is downloaded through `GET /compress/download`
- To maximize compression, the intermediate file should be compressed again by a general-purpose file compressor desired by the user (in the figure below is used `gzip`)

![logcomp_steps](https://github.com/EnacheAlexandru/LogCompressor/assets/63500798/ae87adba-5fb3-4047-9047-b3e8e7174235)

## Decompression steps

- The intermediate file is sent through `POST /decompress` endpoint. At this step, `LogComp` is reconstructing the original log file
- The reconstructed log file generated by `LogComp` is downloaded through `GET /decompress/download`

> Note: In order for the reconstructed log file to be identical to the original log file, make sure that the original log file ends with a newline. `LogComp` has difficulties reconstructing the last log line.

## Other configurations

Different parameters can be changed inside the configuration file `application.properties`. These are some (default) examples:

-  `logcompressor.newline.marker=$n$` - used for log lines that do not match the regex statement (ex. stacktraces). It is important to keep in mind that the content of the log file should not contain the value of this parameter 
- `logcompressor.num.separators=[:,.]` - regex statement; possible separators that can appear in sequences of `num`  (ex. a timestamp such as `19:26:21,044`)
- `logcompressor.line.separators=[\\p{Punct}\\p{Blank}]` - regex statement; possible separators that can appear inside the regex statement that has its capturing groups replaced by the type of repetitiveness identifiers (i.e. `rep num - dict [dict:dict@num] - msg`)
- `logcompressor.compressed.log.filename=log-compressed.log` - name of the intermediate file after compression
- `logcompressor.decompressed.log.filename=log-decompressed.log` - name of the the reconstructed log file after decompression
- `logcompressor.debug.print.line.multiple=100000` - used for debugging and it is only relevant when the application's log level is set to `DEBUG`. It logs the current read line during compression/decompression in multiples of the set value. Lower values mean higher granularity for better investigation, but may lead to a decrease performance regarding speed

## Dataset

The dataset that was used in testing this application and which is located in the `resources` directory of `LogComp` was retrieved from [Loghub](https://github.com/logpai/loghub), which is a GitHub repository that contains log file samples collected from systems that are freely available for research or academic work.

Loghub paper citation: Jieming Zhu, Shilin He, Pinjia He, Jinyang Liu, Michael R. Lyu. Loghub: A Large  Collection of System Log Datasets for AI-driven Log Analytics. In ISSRE, 2023.

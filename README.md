# WARNING: Project is put on hold for a while. All development efforts are put on [Pragmatica Lite](https://github.com/siy/pragmatica-lite).

[![](https://jitpack.io/v/siy/pragmatica.svg)](https://jitpack.io/#siy/pragmatica)
[![](https://jitci.com/gh/siy/pragmatica/svg)](https://jitci.com/gh/siy/pragmatica)

![GitHub stars](https://img.shields.io/github/stars/siy/pragmatica?style=social)
![GitHub forks](https://img.shields.io/github/forks/siy/pragmatica?style=social)
![GitHub watchers](https://img.shields.io/github/watchers/siy/pragmatica?style=social)
![GitHub followers](https://img.shields.io/github/followers/siy?style=social)

![GitHub repo size](https://img.shields.io/github/repo-size/siy/pragmatica?style=plastic)
![GitHub language count](https://img.shields.io/github/languages/count/siy/pragmatica?style=plastic)
![GitHub top language](https://img.shields.io/github/languages/top/siy/pragmatica?style=plastic)
![GitHub last commit](https://img.shields.io/github/last-commit/siy/pragmatica?color=red&style=plastic)

<a href="https://sergiy-yevtushenko.medium.com/"><img src="https://img.shields.io/badge/medium-%2312100E.svg?&style=for-the-badge&logo=medium&logoColor=white" height=25></a>
<a href="https://dev.to/siy"><img src="https://img.shields.io/badge/DEV.TO-%230A0A0A.svg?&style=for-the-badge&logo=dev-dot-to&logoColor=white" height=25></a>

# Pragmatic Functional Java Essentials

Minimal set of Java classes necessary to apply [Pragmatic Functional Java](https://github.com/siy/pragmatica/wiki)
approaches in practice.

Current version requires _Java 17_ to build and run.

Instead of using this library as a dependency, it is highly suggested to just copy classes into your own codebase and
adapt it to your needs.

## Performance Implications

In order to avoid performance penalty caused by using Pragmatic Functional Java approaches, all classes were designed
with performance in mind. In particular, despite inherently conditional behavior (empty vs nothing Option, success vs
failure Result), implementation does not use branching operator (`if` or `?` ). Java compiler and JIT do the rest -
resulting performance virtually identical to traditional Java code with explicit `null` checks and exceptions.

It should be noted, that `Result` benchmark puts exception handling cases into rather convenient conditions, with very
short call stack and only one type of exception is generated. Exception stack trace is not accessed either. All these
conditions enabling much better optimization and don't trigger some resource-intensive processing (like printing stack
trace) which usually happens (for example, for logging) in real applications.

Benchmarks measure execution of same code under different rate of missing (error) cases. Rate set to 0% is equal to
`happy day scenraio` when there is no empty values (or errors/exceptions). With 100% rate all cases are empty/null
(error/exception) and usually show plain overhead caused by used handling method.

Test results were obtained on MacBook Pro M1. Results are reorganized to simplify direct comparison of different
implementations for same use case.

Test results for `Option`:

```
Benchmark                           Mode  Cnt   Score    Error  Units

OptionPerformanceTest.nullable0    avgt    6  36.107 ±  0.456  us/op
OptionPerformanceTest.option0      avgt    6  35.085 ±  0.409  us/op

OptionPerformanceTest.nullable10   avgt    6  32.365 ±  0.064  us/op
OptionPerformanceTest.option10     avgt    6  31.938 ±  0.323  us/op

OptionPerformanceTest.nullable25   avgt    6  26.910 ±  0.828  us/op
OptionPerformanceTest.option25     avgt    6  26.347 ±  0.113  us/op

OptionPerformanceTest.nullable50   avgt    6  18.158 ±  0.119  us/op
OptionPerformanceTest.option50     avgt    6  17.688 ±  0.086  us/op

OptionPerformanceTest.nullable75   avgt    6   9.146 ±  0.198  us/op
OptionPerformanceTest.option75     avgt    6   8.844 ±  0.181  us/op

OptionPerformanceTest.nullable90   avgt    6   3.716 ±  0.022  us/op
OptionPerformanceTest.option90     avgt    6   3.599 ±  0.055  us/op

OptionPerformanceTest.nullable100  avgt    6   0.084 ±  0.001  us/op
OptionPerformanceTest.option100    avgt    6   0.087 ±  0.008  us/op
```

Test results for `Result`:

```
Benchmark                           Mode  Cnt   Score    Error  Units

ResultPerformanceTest.exception0    avgt    6  36.621 ±  0.045  us/op
ResultPerformanceTest.result0       avgt    6  35.215 ±  0.208  us/op

ResultPerformanceTest.exception10   avgt    6  32.651 ±  0.129  us/op
ResultPerformanceTest.result10      avgt    6  31.983 ±  0.112  us/op

ResultPerformanceTest.exception25   avgt    6  27.373 ±  0.240  us/op
ResultPerformanceTest.result25      avgt    6  26.472 ±  0.130  us/op

ResultPerformanceTest.exception50   avgt    6  18.239 ±  0.769  us/op
ResultPerformanceTest.result50      avgt    6  17.671 ±  0.101  us/op

ResultPerformanceTest.exception75   avgt    6   9.213 ±  0.597  us/op
ResultPerformanceTest.result75      avgt    6   9.019 ±  0.090  us/op

ResultPerformanceTest.exception90   avgt    6   3.705 ±  0.021  us/op
ResultPerformanceTest.result90      avgt    6   3.618 ±  0.019  us/op

ResultPerformanceTest.exception100  avgt    6   0.087 ±  0.001  us/op
ResultPerformanceTest.result100     avgt    6   0.086 ±  0.001  us/op
```

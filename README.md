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

# Pragmatica -  Pragmatic Functional Java Essentials & Modern Asynchronous I/0

_Pragmatica_ is a set of necessary libraries to write Java application in a modern style.
At present, it consists of the following components:
 - Pragmatica Core - minimal set of classes for [Pragmatic Functional Java](https://github.com/siy/pragmatica/wiki) style.
 - Proactor - implementation of the low level infrastructure for fast task scheduler and [io_uring](https://unixism.net/loti/index.html) - based asynchronous I/O.
 - Async I/O - implementation of Promise-based asynchronous API and basic utility classes.

Together these libraries provide all basic components necessary for completely asynchronous I/O operations, including network communication and file processing. 

At present Pragmatica does not have higher level elements like support for SSL/TLS or HTTP protocol.

Only minimal set of I/O operations is supported at the moment. Support for remaining operations available via _io_uring_ API is planned for next releases. 

## Requirements

- _Pragmatica_ uses _Java 17_. There are no plans to support older versions of Java.
- _Pragmatica Core_ has no specific requirements. 
- _Pragmatica Proactor_ and _Pragmatica Async I/O_ components require Linux kernel version at least 5.6. Best results could be achieved with kernel versions 5.16 and up. Other operating systems are not supported and there are no plans for such a support.  

Note that if you want/need to use other operating system to work on Pragmatica code or build binaries, you can use [Pragmatica Builder Image](./docker/README.md).

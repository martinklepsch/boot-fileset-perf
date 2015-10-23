# boot fileset perf

### Impact of number of files in fileset

```
1. ./add-files 100
2. boot watch pass
3. touch input/34.txt

Repeat Step 3, average
```

**See build.boot for a lazier/faster copy-with-lastmod implementation**
This requires [this line](https://github.com/boot-clj/boot/blob/master/boot/pod/src/boot/tmpdir.clj#L67) to be removed from boot's fileset `commit!` code.

### Impact of number of files modified by task

untested, `modify-random` task can be used

### Impact of number of tasks in pipeline

untested, `modify-random` task can be used

### Impact of excluding dirs from watching

minimal...

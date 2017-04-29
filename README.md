artags
======

Generate tags file for arxml.

Usage:
------

```sh
java -jar artags-x.x.x.jar PATH_TO_BASE_DIR

java -jar artags-x.x.x.jar PATH_TO_BASE_DIR -e EXCLUDE_PATTERN
```


Requirements:
-------------

- java version "1.8.0_112" or later.


Feature :
---------

- [ ] : 参照先(<XXX-REF> タグで指定されたエレメント) にジャンプするための tags ファイルを生成する
- [x] : 複数 arxml の入力に対応する
- [x] : ディレクトリを指定すると、それ以下の arxml ファイルを再帰的に探す
- [ ] : 検索対象外ディレクトリを指定できる


License:
--------

Copyright (C) 2017 mikoto2000

This software is released under the MIT License, see LICENSE

このソフトウェアは MIT ライセンスの下で公開されています。 LICENSE を参照してください。


Author:
-------

mikoto2000 <mikoto2000@gmail.com>


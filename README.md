# protomic
[![Clojars Project](https://img.shields.io/clojars/v/com.github.hden/protomic.svg)](https://clojars.org/com.github.hden/protomic)

Async client API for Datomic with Promesa.

## Install

`[com.github.hden/protomic "0.1.0-SNAPSHOT"]`

or

`com.github.hden/protomic {:mvn/version "0.1.0-SNAPSHOT"}`

## Usage

```clj
(require '[promesa.core :as p])
(require '[datomic.client.api.async :as d])
(require '[protomic.core :as core])

(def client
  (d/client {:server-type :dev-local
             :storage-dir :mem
             :system      "dev"}))

@(p/chain (core/connect client {:db-name "dev"})
    d/db
    #(core/q {:args [%]
              :query '[:find ?tx-inst
                       :where [_ :db/txInstant ?tx-inst]]})
    #(map first %))
;; (#inst "2021-10-10T01:15:08.507-00:00" #inst "2021-10-10T01:15:08.517-00:00" #inst "1970-01-01T00:00:00.000-00:00")
```

## License

Copyright © 2021 Haokang Den

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

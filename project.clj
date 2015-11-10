(defproject om-tutorial "0.1.0-SNAPSHOT"
  :description "My first Om program!"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.omcljs/om "1.0.0-alpha20-SNAPSHOT"]
                 [datascript "0.13.2"]
                 [figwheel-sidecar "0.5.0-SNAPSHOT" :scope "provided"]]



  :clean-targets ^{:protect false} ["resources/public/js" "target"])
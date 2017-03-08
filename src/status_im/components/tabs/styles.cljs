(ns status-im.components.tabs.styles
  (:require [status-im.components.styles :as st]
            [status-im.utils.platform :as p]))


(def tabs-height 56)
(def tab-height (dec tabs-height))

(def bottom-gradient
  {:position :absolute
   :bottom   55
   :left     0
   :right    0})

(defn tabs-container [hidden?]
  {:position         :absolute
   :bottom           0
   :left             0
   :right            0
   :height           tabs-height
   :background-color st/color-white
   :margin-bottom    (if hidden? (- tabs-height) 0)
   :transform        [{:translateY 1}]})

(def tabs-container-line
  {:border-top-width 1
   :border-top-color "#D7D7D7"})

(def tabs-inner-container
  {:flexDirection   :row
   :height          tab-height
   :opacity         1
   :justifyContent  :center
   :alignItems      :center})

(def tab
  {:flex           1
   :height         tab-height
   :justifyContent :center
   :alignItems     :center})

(defn tab-title [active?]
  {:font-size  (if-not (or active? p/ios?) 12 14)
   :height     16
   :min-width  60
   :text-align :center
   :color      (if active? st/color-light-blue st/color-gray4)})

(def tab-icon
  {:width        24
   :height       24
   :marginBottom 1
   :align-self   :center})

(def tab-container
  {:flex             1
   :height           tab-height
   :justifyContent   :center
   :alignItems       :center})

(def main-swiper
  {:position         :absolute
   :top              0
   :left             0
   :right            0
   :bottom           tabs-height
   :shows-pagination false})

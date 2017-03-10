(ns status-im.profile.screen
  (:require-macros [status-im.utils.views :refer [defview]])
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]
            [cljs.spec :as s]
            [reagent.core :as r]
            [status-im.contacts.styles :as cst]
            [status-im.components.context-menu :refer [context-menu]]
            [status-im.components.react :refer [view
                                                text
                                                text-input
                                                image
                                                icon
                                                modal
                                                scroll-view
                                                touchable-highlight
                                                touchable-opacity
                                                touchable-without-feedback
                                                show-image-picker
                                                dismiss-keyboard!]]
            [status-im.components.icons.custom-icons :refer [oct-icon]]
            [status-im.components.chat-icon.screen :refer [my-profile-icon]]
            [status-im.components.status-bar :refer [status-bar]]
            [status-im.components.toolbar-new.view :refer [toolbar]]
            [status-im.components.toolbar-new.actions :as act]
            [status-im.components.text-field.view :refer [text-field]]
            [status-im.components.selectable-field.view :refer [selectable-field]]
            [status-im.components.status-view.view :refer [status-view]]
            [status-im.components.list-selection :refer [share]]
            [status-im.utils.phone-number :refer [format-phone-number]]
            [status-im.utils.image-processing :refer [img->base64]]
            [status-im.utils.platform :refer [platform-specific]]
            [status-im.profile.handlers :refer [message-user]]
            [status-im.profile.validations :as v]
            [status-im.profile.styles :as st]
            [status-im.utils.random :refer [id]]
            [status-im.utils.utils :refer [clean-text]]
            [status-im.components.image-button.view :refer [show-qr-button]]
            [status-im.i18n :refer [label
                                    get-contact-translated]]
            [status-im.constants :refer [console-chat-id wallet-chat-id]]
            [status-im.utils.datetime :as time]))


(defn status-image-view [_]
  (let [component         (r/current-component)
        just-opened?      (r/atom true)
        input-ref         (r/atom nil)
        set-status-height #(let [height (-> (.-nativeEvent %)
                                            (.-contentSize)
                                            (.-height))]
                            (r/set-state component {:height height}))]
    (r/create-class
      {:reagent-render
       (fn [{{:keys [whisper-identity
                     name
                     status
                     photo-path]} :account
             edit?                :edit?}]
         [view st/status-block
          [view st/user-photo-container

           (if edit?
             [touchable-highlight {:on-press (fn []
                                               (let [list-selection-fn (:list-selection-fn platform-specific)]
                                                 (dispatch [:open-image-source-selector list-selection-fn])))}
              [view
               [my-profile-icon {:account {:photo-path photo-path
                                           :name       name}
                                 :edit?   edit?}]]]
             [my-profile-icon {:account {:photo-path photo-path
                                         :name       name}
                               :edit?   edit?}])]
          [text-field
           {:line-color       :white
            :focus-line-color :white
            :editable         edit?
            :input-style      (st/username-input edit? (s/valid? ::v/name name))
            :wrapper-style    st/username-wrapper
            :value            (get-contact-translated whisper-identity :name name)
            :on-change-text   #(dispatch [:set-in [:profile-edit :name] %])}]
          (if (or edit? @just-opened?)
            [text-input {:ref                    #(reset! input-ref %)
                         :style                  (st/status-input (:height (r/state component)))
                         :multiline              true
                         :editable               true
                         :on-content-size-change #(do (set-status-height %)
                                                      (reset! just-opened? false))
                         :max-length             140
                         :placeholder            (label :t/profile-no-status)
                         :on-change-text         #(let [status (clean-text %)]
                                                    (if (str/includes? % "\n")
                                                      (.blur @input-ref)
                                                      (dispatch [:set-in [:profile-edit :status] status])))
                         :default-value          status}]
            [status-view {:style  (st/status-text (:height (r/state component)))
                          :status status}])])})))

(defview my-profile-old []
  [edit? [:get-in [:profile-edit :edit?]]
   qr [:get-in [:profile-edit :qr-code]]
   current-account [:get-current-account]
   changed-account [:get :profile-edit]]
  {:component-will-unmount #(dispatch [:set-in [:profile-edit :name] nil])}
  (let [{:keys [phone
                address
                public-key]
         :as account} (if edit?
                        changed-account
                        current-account)]
    [scroll-view {:style   st/profile
                  :bounces false}
     [status-bar]
     [toolbar {:account account
                   :edit?   edit?}]

     [status-image-view {:account account
                         :edit?   edit?}]

     [scroll-view (merge st/my-profile-properties-container {:bounces false})
      [view st/profile-property
       [selectable-field {:label     (label :t/phone-number)
                          :editable? edit?
                          :value     (if (and phone (not (str/blank? phone)))
                                       (format-phone-number phone)
                                       (label :t/not-specified))
                          :on-press  #(dispatch [:phone-number-change-requested])}]
       [view st/underline-container]]

      [view st/profile-property
       [view st/profile-property-row
        [view st/profile-property-field
         [selectable-field {:label     (label :t/address)
                            :editable? edit?
                            :value     address
                            :on-press  #(share address (label :t/address))}]]
        [show-qr-button {:handler #(dispatch [:navigate-to-modal :qr-code-view {:contact   account
                                                                                :qr-source :address}])}]]
       [view st/underline-container]]

      [view st/profile-property
       [view st/profile-property-row
        [view st/profile-property-field
         [selectable-field {:label     (label :t/public-key)
                            :editable? edit?
                            :value     public-key
                            :on-press  #(share public-key (label :t/public-key))}]]
        [show-qr-button {:handler #(dispatch [:navigate-to-modal :qr-code-view {:contact   account
                                                                                :qr-source :public-key}])}]]]

      [view st/underline-container]]]))

;;TODO it should be moved to components
;;TODO =--------------------------------------
(defn separator []
  [view cst/contact-item-separator-wrapper
   [view cst/contact-item-separator]])

;;TODO it should be moved to components
(defn settings-button [label icon-key on-press]
  [touchable-highlight {:on-press on-press}
   [view st/settings-group-item
    [view st/settings-icon-container
     [icon icon-key]]
    [view st/settings-group-text-container
     [text {:style st/settings-group-text}
      label]]]])
;;TODO =--------------------------------------

(defview my-prifile-toolbar []
  [toolbar {:actions [(act/opts {:value #(dispatch [:open-edit-profile])
                                 :text (label :t/edit)})]}])
(defn online-text [last-online]
  (let [last-online-date (time/to-date last-online)
        now-date         (time/now)]
    (if (and (pos? last-online)
             (<= last-online-date now-date))
      (time/time-ago last-online-date)
      (label :t/active-unknown))))

(defn profile-bage [{:keys [name last-online] :as contact}]
  [view {:align-items :center
         :padding-top 24}
   [my-profile-icon {:account contact
                     :edit?   false}]
   [view {:margin-top 12}
    [text {:style {:font-size 17 :line-height 20 :letter-spacing -0.2}}
     name]]
   (when-not (nil? last-online)
     [view {:margin-top 4}
      [text {:style {:font-size 14 :line-height 20 :letter-spacing -0.2 :color "#939ba1"}}
       (online-text last-online)]])])

(defn add-to-contacts [pending? chat-id]
  [view
   (if pending?
     [touchable-highlight {:on-press #(dispatch [:add-pending-contact chat-id])}
      [view st/add-to-contacts
       [text {:style st/add-to-contacts-text}
        (label :t/add-to-contacts)]]]
     [view st/in-contacts
      [icon :ok_blue]
      [view {:align-items :center
             :flex 1}
       [text {:style st/in-contacts-text}
        (label :t/in-contacts)]]])])

(defn profile-actions [whisper-identity chat-id]
  [view {:padding-top 10}
   [settings-button (label :t/start-conversation)
                    :chats_blue
                    #(message-user whisper-identity)]
   [separator]
   [settings-button (label :t/send-transaction)
                    :arrow_right_blue
                    #(dispatch [:open-chat-with-the-send-transaction chat-id])]])

(defn profile-setting-item [label value options text-mode]
  [view {:padding-left 16
         :padding-right 16
         :flex-direction :row
         :align-items :center
         :height 73}
   [view {:flex 1
          :padding-right 20}
    [text {:style {:font-size 14
                   :letter-spacing -0.2
                   :color "#939ba1"}}
     label]
    [view {:height 10}]
    [text {:numberOfLines 1
           :ellipsizeMode text-mode
           :style {:font-size 17
                   :letter-spacing -0.2}}
     value]]
   (when options
     [view
      [context-menu
       [icon :options_gray]
       options]])])

(defn show-qr [contact qr-source]
  #(dispatch [:navigate-to-modal :qr-code-view {:contact   contact
                                                :qr-source :address}]))

(defn profile-info [{:keys [whisper-identity :whisper-identity
                            address          :address
                            status           :status
                            phone            :phone] :as contact}]
  [view
   [profile-setting-item (label :t/status) status]
   [view st/settings-separator]
   [profile-setting-item (label :t/address) address [{:value (show-qr contact :address)
                                                      :text (label :t/show-qr) :middle}]]
   [view st/settings-separator]
   [profile-setting-item (label :t/public-key) whisper-identity [{:value (show-qr contact :public-key)
                                                                  :text (label :t/show-qr) :middle}]]
   [view st/settings-separator]
   [profile-setting-item (label :t/phone-number) phone]])

(defn my-profile-info [{:keys [public-key :public-key
                               address    :address
                               status     :status
                               phone      :phone :as contact]}]
  [view
   [profile-setting-item (label :t/status) status [{:value #()
                                                    :text (label :t/edit)}]]
   [view st/settings-separator]
   [profile-setting-item (label :t/address) address [{:value (show-qr contact :address)
                                                      :text (label :t/show-qr) :middle}]]
   [view st/settings-separator]
   [profile-setting-item (label :t/public-key) public-key [{:value (show-qr contact :public-key)
                                                            :text (label :t/show-qr) :middle}]]
   [view st/settings-separator]
   [profile-setting-item (label :t/phone-number) phone [{:value #(dispatch [:phone-number-change-requested])
                                                         :text (label :t/edit)}]]])

(defview my-profile []
  [current-account [:get-current-account]]
  [view {:background-color "#eef2f5"}
   [status-bar]
   [toolbar]
   [view {:background-color "#ffffff"}
    [profile-bage current-account]
    [view st/share-qr-separator]
    [settings-button (label :t/share-qr)
                     :q_r_blue
                     (show-qr current-account :public-key)]]
   [view {:background-color "#ffffff"
          :margin-top 16}
    [my-profile-info current-account]]])

(defview profile []
  [{:keys [pending?
           whisper-identity]
    :as contact} [:contact]
   chat-id [:get :current-chat-id]]
  [view {:background-color "#eef2f5"}
   [status-bar]
   [toolbar]
   [scroll-view
    [view {:background-color "#ffffff"}
     [profile-bage contact]
     [add-to-contacts pending? chat-id]
     [profile-actions whisper-identity chat-id]]
    [view {:background-color "#ffffff"
           :margin-top 16}
     [profile-info contact]]]])
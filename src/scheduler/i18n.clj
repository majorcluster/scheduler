(ns scheduler.i18n)

(def t
  {:en-US {:verify-email-subject "Hello there! Please verify your e-mail!"
           :verify-email-body "Welcome {{name}}! <br/>Please verify your e-mail by clicking <a href=\"{{link}}\">this link</a>."
           :recovery-password-subject "Password recovery"
           :recovery-password-body "Hello {{name}}! <br/>To recover your password, click <a href=\"{{link}}\">here</a>."
           :admin-signup-subject "Hello there! Please verify your e-mail!"
           :admin-signup-body "Welcome! <br/>Please verify your e-mail by clicking <a href=\"{{link}}\">this link</a>."}
   :pt-BR {:verify-email-subject "Bem vind@! Por favor, verifique seu e-mail!"
           :verify-email-body "Bem vindx {{name}}! <br/>Por favor, verifique seu e-mail clicando <a href=\"{{link}}\">neste link</a>."
           :recovery-password-subject "Recuperação de senha"
           :recovery-password-body "Olá {{name}}! <br/>Para recuperar sua senha, clique <a href=\"{{link}}\">aqui</a>."
           :admin-signup-subject "Bem vind@! Por favor, verifique seu e-mail!"
           :admin-signup-body "Bem vindx! <br/>Por favor, verifique seu e-mail clicando <a href=\"{{link}}\">neste link</a>."}})

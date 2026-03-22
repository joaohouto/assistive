# Assistive Menu Tool

Botão flutuante de acessibilidade para Android. Exibe um botão arrastável sobre qualquer tela que, ao ser tocado, abre um menu com ações rápidas do sistema — similar ao recurso de acessibilidade disponível em outros sistemas operacionais móveis.

---

## Funcionalidades

- **Botão flutuante** arrastável por toda a tela
- **Snap automático** para a borda mais próxima ao soltar
- **Menu expansível** com animação de escala + fade
- **10 ações disponíveis**, configuráveis pelo usuário:

| Ação | Descrição |
|---|---|
| Início | Vai para a tela inicial |
| Voltar | Equivale ao botão Voltar |
| Recentes | Abre apps recentes |
| Bloquear | Bloqueia a tela (Android 9+) |
| Volume + | Aumenta o volume |
| Volume − | Diminui o volume |
| Screenshot | Captura a tela (Android 9+) |
| Notificações | Abre a gaveta de notificações |
| Painel Rápido | Abre as configurações rápidas |
| Configurar | Abre o app para ajustar as opções |

- **Configurações persistentes** (SharedPreferences):
  - Opacidade do botão (30 % – 100 %)
  - Tamanho do botão (Pequeno / Médio / Grande)
  - Seleção das ações exibidas no menu

---

## Requisitos

| Item | Valor |
|---|---|
| Android mínimo | 7.0 (API 24) |
| Android alvo | 15 (API 36) |
| Kotlin | 2.2.10 |
| UI | Jetpack Compose + Material 3 |

---

## Permissões necessárias

O app solicita duas permissões especiais que precisam ser concedidas manualmente:

### 1. Sobreposição de apps (`SYSTEM_ALERT_WINDOW`)
Permite que o botão flutuante apareça sobre outros aplicativos.
→ **Configurações › Apps › Assistive Menu Tool › Aparecer na parte superior**

### 2. Serviço de acessibilidade (`BIND_ACCESSIBILITY_SERVICE`)
Permite executar ações globais do sistema (Home, Voltar, Recentes, Bloquear, Screenshot).
→ **Configurações › Acessibilidade › Assistive Menu Tool › Ativar**

> O app guia o usuário pelas duas permissões na tela principal.

---

## Arquitetura

```
app/src/main/java/com/joaohouto/assistivemenutool/
├── MainActivity.kt                    # Tela principal (permissões + configurações)
├── FloatingButtonService.kt           # Foreground Service — overlay WindowManager
├── AssistiveMenuAccessibilityService.kt # AccessibilityService — ações globais
├── MenuAction.kt                      # Enum com todas as ações disponíveis
├── SettingsRepository.kt              # Persistência via SharedPreferences
└── ui/theme/                          # Jetpack Compose Material 3 theme
```

### Componentes principais

**`FloatingButtonService`**
- Adiciona o botão ao `WindowManager` com `TYPE_APPLICATION_OVERLAY`
- Gerencia arrastar, soltar e snap animado para a borda
- Abre/fecha o menu com animação de escala
- Ouve mudanças de configurações em tempo real via `OnSharedPreferenceChangeListener`
- Ações de volume via `AudioManager`; Screenshot com delay de 400 ms

**`AssistiveMenuAccessibilityService`**
- Executa `performGlobalAction()` para Home, Voltar, Recentes, Bloquear, Screenshot, Notificações e Painel Rápido
- Expõe `instance` via companion object para comunicação com o `FloatingButtonService`

**`SettingsRepository`**
- Wrapper de `SharedPreferences` com propriedades tipadas (`opacity`, `buttonSizeDp`, `menuActions`)
- `menuActions` serializado como CSV de nomes do enum

---

## Build & Release

### Debug
Abra o projeto no Android Studio e clique em **Run**.

### Release
1. Gere um keystore de assinatura:
   ```bash
   keytool -genkey -v -keystore release.jks -alias amt -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Configure a assinatura em `app/build.gradle.kts`:
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("release.jks")
           storePassword = "..."
           keyAlias = "amt"
           keyPassword = "..."
       }
   }
   ```
3. Gere o APK/AAB:
   ```bash
   ./gradlew bundleRelease   # AAB para Google Play
   ./gradlew assembleRelease # APK direto
   ```

> O build de release tem R8 (`isMinifyEnabled = true`) e remoção de recursos não utilizados (`isShrinkResources = true`) ativados.

---

## Licença

```
MIT License

Copyright (c) 2026 João Houto

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

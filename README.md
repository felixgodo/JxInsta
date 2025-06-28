<p align="center"><img alt="JxInsta" height="300" src="/JxInsta.png"></p>

<p align="center">
  <img src="https://img.shields.io/github/license/ErrorxCode/JxInsta?style=for-the-badge">
  <img src="https://img.shields.io/github/stars/ErrorxCode/JxInsta?style=for-the-badge">
  <img src="https://img.shields.io/github/issues/ErrorxCode/JxInsta?color=red&style=for-the-badge">
  <img src="https://img.shields.io/github/forks/ErrorxCode/JxInsta?color=teal&style=for-the-badge">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Author-Rahil-cyan?style=flat-square">
  <img src="https://img.shields.io/badge/Open%20Source-Yes-cyan?style=flat-square">
  <img src="https://img.shields.io/badge/Written%20In-Java-cyan?style=flat-square">
</p>

---

# 🚀 JxInsta

**JxInsta** is a modern, lightweight Java wrapper for Instagram’s private web API (with potential support for mobile API in the future).  
It’s designed as a clean and reliable replacement for **instagram4j**, which is outdated and suffered from common header issues.

> 🔧 This library is developed under the **[EasyInsta](https://github.com/ErrorxCode/EasyInsta)** project umbrella.

---

## ✨ Features

- ✅ Lightweight, object-oriented, and easy to use
- 🚫 No API token required
- 💬 Send, receive, and delete messages
- 📡 *Realtime direct message listener* (**pending**)
- 🔐 Login with session cache
- 📷 Post photos and stories
- ➕ Follow / ➖ Unfollow users
- 👤 Accept / ignore follow requests (**pending**)
- 📈 Scrape followers / followings
- 👁️ View profile info
- ❤️ Like / comment on posts
- 📰 Get feed / timeline
- 💾 Download posts and profile pictures

---

## 📦 Adding the Project as a Dependency

To use this project as a dependency, simply clone the repository and publish it to your local Maven repository:

```bash
./gradlew publishToMavenLocal
```

Then, in your project's `build.gradle`, include:

```groovy
repositories {
    mavenLocal()
    // other repositories if needed
}
```

And add the dependency:

```groovy
dependencies {
    implementation 'com.example:jxinsta:1.0.0'
}
```

---

## 📚 Documentation

Full usage instructions are available in the [📖 User Guide](https://github.com/ErrorxCode/JxInsta/wiki).

---

## 💡 Example

```java
public class Test {
    public static void main(String[] args) {
        final JxInsta insta = new JxInsta("username", "password", JxInsta.LoginType.WEB_AUTHENTICATION);
        insta.uploadStory(new File("photos/story-24.png"));
        var profile = insta.getProfile("username");
        // Other actions...
    }
}
```
---

## 🙌 Acknowledgements

- 📌 [Instagram usage limits](https://www.linkedin.com/pulse/stay-within-boundaries-complete-breakdown-instagrams-cmscc/)
- ⏱️ [Instagram daily limit](https://socialpros.co/instagram-daily-limits/#:~:text=Instagram's%20Daily%20Limits%20%E2%80%93%20Like,than%2030%20likes%20per%20hour)
- 📜 [API Policies](https://developers.facebook.com/devpolicy/)
- 🛑 [Instagram checkpoints & challenges](https://github.com/ErrorxCode/JxInsta/blob/main/Instagram%20checkpoints.md)

---

## 🙋‍♂️ FAQ

**Q1. Can we use this library to make bots?**  
✅ Yes. While Instagram prohibits bots through its official APIs, this is a private API. Use responsibly and stay within usage limits.

**Q2. Can we download stories or posts with this library?**  
✅ Yes — even without logging in.

**Q3. Does the library require tokens or keys?**  
❌ No tokens needed — just username and password (or login via cookies / bearer token).

**Q4. Can we use WebView to login on Android?**  
✅ Yes. See this [example](https://github.com/ErrorxCode/JxInsta/wiki/Android-users#using-webview-for-login).

---

## 🤝 Contributing

Contributions are always welcome!  
Check out the [Contribution Guide](https://github.com/ErrorxCode/JxInsta/blob/main/CONTRIBUTING.md), the [Code of Conduct](https://github.com/ErrorxCode/JxInsta/blob/main/CODE_OF_CONDUCT.md), and the [Todo list](https://github.com/ErrorxCode/JxInsta/blob/main/Todo.md) for ideas on how to help.

---

## 💬 Support

If you like this project, don’t forget to ⭐ the repository!

---
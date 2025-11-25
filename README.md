# 🌌 Nyctotile

**An open-source voxel-like game with mysterious lore.**

Nyctotile is an early-stage project building a modern, moddable, voxel-based adventure game written entirely in Java. Explore a world shrouded in mystery, uncover its history, and survive the secrets hidden in the shadows.

## 🛠️ Project Status

| Status                            | Language & Dependencies | License         |
|:----------------------------------|:------------------------|:----------------|
| **Early Development (Pre-Alpha)** | **Java 17** (LWJGL3)    | **MIT License** |

Currently, the project is focusing on core engine stability and fundamental game logic. It is not yet ready for end-user installation.

-----

## 🚀 Key Technologies

Nyctotile is built on a modern Java foundation, leveraging performance and portability.

| Technology  | Purpose                                                                    |
|:------------|:---------------------------------------------------------------------------|
| **Java 17** | Primary development language.                                              |
| **LWJGL3**  | Low-level access to graphics (OpenGL), audio, and input.                   |
| **JOML**    | Fast and lightweight mathematics library for vector and matrix operations. |
| **TOML**    | Configuration file format for easy, human-readable settings.               |

-----

## 💻 Getting Started (For Developers)

Since the project is in a very early stage, there is no runnable game distribution yet. However, developers can clone the repository to review, build, and contribute to the core engine.

### Prerequisites

* A Java Development Kit (**JDK 17** or higher).
* A preferred IDE (e.g., IntelliJ IDEA, Eclipse) with **Gradle** support.

### Running the Project

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/infinitytwogames-web/Nyctotile.git
    cd Nyctotile
    ```
2.  **Open in IDE:**
    Import the project into your IDE as a **Gradle project**.
3.  **Run Main Class:**
    Locate the main entry point (`org.infinitytwo.umbralore.core.debug.Main` for 3D only, or `org.infinitytwo.umbralore.core.Main` for the actual game) and run it directly. 

-----

## 👋 Contribution

We welcome contributions\! As a project in early development, all contributions are valuable, from code implementation to bug reporting.

### How to Contribute

1.  **Report Issues:** Found a bug? Please open a detailed **Issue** on the repository. Include the full stacktrace and steps to reproduce.
2.  **Submit Code:** Fork the repository, create a descriptive branch, and submit a **Pull Request (PR)** with your changes. New features or bug fixes should align with the project's architectural principles.

### Licensing

This project is licensed under the **MIT License**. This is a permissive license that allows for great flexibility, including commercial use, modification, and private distribution.

See the `LICENSE` file in the repository root for full details.
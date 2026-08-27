# Techsenger Weaverbird

Techsenger Weaverbird is a framework built on top of the Java Platform Module System (JPMS) that manages modular components
through dynamic module layers. It provides a powerful API and versatile interfaces (CLI/GUI) with multiple built-in
commands — helping developers efficiently build and manage modular systems.

## Table of Contents
* [Overview](#overview)
* [Use Cases](#use-cases)
* [Demo](#demo)
    * [CLI Demo](#demo-cli)
    * [GUI Demo](#demo-gui)
* [Usage](#usage)
    * [Framework](#usage-framework)
        * [Directory Layout](#usage-framework-directory)
        * [Registry](#usage-framework-registry)
        * [Boot](#usage-framework-boot)
    * [Component](#usage-component)
        * [Life Cycle](#usage-component-life)
        * [Activator](#usage-component-activator)
        * [Configuration](#usage-component-config)
        * [Events](#usage-component-events)
        * [Services](#usage-component-services)
    * [Remote Control](#usage-remote-control)
    * [Text Commands](#usage-commands)
    * [CLI](#usage-cli)
    * [GUI](#usage-gui)
        * [Console](#usage-gui-console)
        * [Memory Log](#usage-gui-memory-log)
        * [Diagrams](#usage-gui-diagrams)
    * [Assembly Maven Plugin](#usage-assembly-plugin)
* [Requirements](#requirements)
* [Dependencies](#dependencies)
* [Code building](#code-building)
* [Running Demo](#running-demo)
* [License](#license)
* [Contributing](#contributing)
* [Support Us](#support-us)

## Overview <a name="overview"></a>

JPMS (Java Platform Module System), which was introduced in Java 9, along with modules, added the concept of module
layer. A layer can be defined as a group of modules that are loaded and managed together. Key features of a layer
include:

* Isolation - modules within a layer can be independent of other layers.
* Hierarchy - layers form a graph and can use modules from parent layers.
* Dynamic configuration - layers can be dynamically added and removed (except the boot layer).

Techsenger Weaverbird is a framework designed to work with module layers. The framework resides in the boot layer and
handles all the work of managing the other layers. To facilitate this, the concept of a component is introduced.

A component is a logical part of the system that can be dynamically added or removed. Each component is deployed in a
separate module layer and has a clearly defined lifecycle. The configuration of a component is specified via a `Builder`
or an XML, which describes the component's modules (groupId, artifactId, version), module directives
(opens, reads, exports, etc.), repositories from which modules can be loaded, and other related information.
For flexibility, the XML configuration supports properties, if and choose-when constructs, and EL (Expression Language).

All loaded modules are stored in the framework's internal repository, which is by default a Maven repository.

The code within a component is started and stopped using activators — special services invoked by the framework during
the activation or deactivation of the component.

You can interact with the framework either through the API or through the text command mechanism. Text commands are a
convenient way to work with the framework. By default, about 40 commands are provided for working with components,
sessions, gathering information, etc. At the same time, it is very easy to add your own commands. The framework will
automatically discover them in any layer (the module must provide a factory service) and integrate them into any
console. The framework can also execute command scripts, which consist of a series of commands, for example, from a file.

The framework provides two types of consoles: CLI and GUI. The CLI console allows working with text commands. The GUI
console additionally allows working with logs and generates diagrams with information about layers, modules, and
packages. These diagrams are especially useful when working with complex systems.

## Use Cases <a name="use-cases"></a>

The framework can be used for programs that:

* Have subsystems that can be dynamically added or removed.
* Support plugins, extensions, add-ons, etc., that can be dynamically loaded.
* Include a web server and web applications, where each web application is a module.
* Use modules that are loaded based on conditions, such as the operating system type, etc.

## Demo <a name="demo"></a>

### CLI Demo <a name="demo-cli"></a>

<img width="1209" height="811" alt="Weaverbird CLI" src="https://github.com/user-attachments/assets/f6dfcc1d-a674-45cd-89e6-fa5ed7a5b6ee" />

### GUI Demo <a name="demo-gui"></a>

<img width="1200" height="800" alt="Weaverbird GUI" src="https://github.com/user-attachments/assets/55986da2-01e0-4c8e-bb85-bec9a02bf2ef" /><br>

<img width="1200" height="800" alt="Weaverbird GUI layer diagram" src="https://github.com/user-attachments/assets/ac9cf773-7615-47f0-8b57-8d58df260e49" /><br>

<img width="1200" height="800" alt="Weaverbird GUI module diagram" src="https://github.com/user-attachments/assets/c02dabee-320f-4723-a767-4ecf0d45628c" /><br>

<img width="1200" height="800" alt="Weaverbird GUI dialog" src="https://github.com/user-attachments/assets/f367ef3b-c938-4c27-afa5-f221e85b2b3e" />

## Usage <a name="usage"></a>

### Framework <a name="usage-framework"></a>

The framework is always located in the root layer. In production, this is the boot layer, but in tests, it can also be
a dynamically created layer. To work with the framework, you need to use the `Framework` class, which provides all the
necessary tools.

It is important to note that when launching the framework, the parameter `--add-modules ALL-DEFAULT` is used, which
loads all default JRE/JDK modules into the boot layer. This is necessary because JRE/JDK modules can only be added to
the boot layer.

#### Directory Layout <a name="usage-framework-directory"></a>

To use the framework in the most efficient way, it is important to carefully design the folder and file structure.
This responsibility is handled by two classes: `PathManager` and `PathResolver` (to create a framework with a custom
`PathManager`, use `FrameworkFactory.create(settings, pathManager)`).

`PathManager` defines paths to the main directories used by the platform, such as `bin`, `cache`, `data`, `repo`, etc.
The `PathResolver`, which can be obtained via `PathManager#getPathResolver()`, is responsible for resolving paths for
a specific component.

The need for `PathResolver` arises from the fact that each component may have not only its own configuration, but
also its own cache, data, documentation, and other resources. Therefore, it is necessary to separate files of
different components within shared directories where component data is stored. Such directories may include:
`cache`, `config`, `data`, `doc`, `legal`.

By default (`DefaultPathResolver`), the framework organizes these directories as follows:

At the root of the folder, framework files are located, while component files are organized into two nested folders.
The name of the first folder is the component name, and the name of the second folder is the component version,
see `PathResolver`.

For example:

```
config
│── Component A
│   │── 1.0.0
│   │   │── configuration.xml   //component file
│   │   │── settings.xml        //component file
│── log4j2.xml                  //framework file
```

#### Registry <a name="usage-framework-registry"></a>

The framework stores data about the installation and components in a registry, which is located in the file
`data/weaverbird-registry.xml`. If the client and server are located in the same folder, as in the `weaverbird-demo-net` example,
both the client and server instances of the framework use this file.

#### Boot <a name="usage-framework-boot"></a>

The boot layer must always contain a minimal set of modules: the two framework modules, the Expression Language modules,
and the logging modules. In addition, the boot layer must contain a module that launches and initializes the framework
— in a distribution this is the main application module, and in integration tests this is the test module itself.

There are two ways to populate the boot layer with the required modules. If the JVM is launched via `.sh`/`.bat`
scripts, the module path is configured by the scripts automatically. The `assemble-dist` goal of the Assembly Maven
Plugin handles this by generating the scripts with the correct module path.

If the JVM is launched via Maven plugins (`exec-maven-plugin`, `maven-failsafe-plugin`, etc.), Maven takes care of
adding the required modules to the boot layer, which makes it convenient both for running the application and for
setting up the integration test environment.

### Component <a name="usage-component"></a>

Each component has a name and a version. The name and version are separated by `:`. For example, the foo component
with version 1.0.0 is specified as `foo:1.0.0`.

An unlimited number of instances of the same component can be created, as instances are identified in the system by
their `id`. Additionally, a component instance can be assigned an `alias` and accessed using this `alias`. This is
especially useful when referencing a component instance in command scripts.

#### Life Cycle <a name="usage-component-life"></a>

Each component can have the following states: `added`, `resolved`, `deployed`, `activated`.

**Added**. The component has been added, but its modules have not yet been loaded from external repositories. The
component can be added either manually or automatically.

To manually add a component, the following steps need to be performed:

1. Create the component's XML configuration.
2. Place the configuration in the special `config` folder.
3. Add the component to the AddedComponents registry of the framework.

The automatic addition of a component occurs when there is a component ZIP archive (with any extension).

The component archive is used for distributing the component. For example, if a program using the framework works with
plugins, each plugin is distributed via its archive. The component archive must always contain the component's
configuration. Additionally, it may contain JAR/WAR modules (which are not in the repository), component data, etc.

The component archive can be created using the `component:build` command, and the component can be added using the
`component:add` command.

**Resolved**. The component's modules have been loaded into the framework's repository, and the component can be deployed.
The resolution of the component is performed using the `component:resolve` command.

**Deployed**. A `ModuleLayer` has been created for the component, which contains all of the component's modules. However,
the component's code has not yet been executed. The deployment of the component is performed using the
`component:deploy` command.

**Activated**. All activators of the component's active modules have been called. The component's code is now being
executed. The activation of the component is performed using the `component:activate` command.

The commands mentioned above allow working with each component state individually; however, in many cases, this is
unnecessary. Therefore, there are combined commands that handle multiple states at once. For example, instead of using
the `component:add` and `component:resolve` commands separately, you can use a single command, `component:install`.

Table of commands and states:

<table>
    <tr>
        <td>Initial State</td>
        <td>Final State</td>
        <td>Command</td>
        <td>Combined Command</td>
    </tr>
    <tr>
        <td>—</td>
        <td>Added</td>
        <td>component:add</td>
        <td rowspan="2">component:install</td>
    </tr>
    <tr>
        <td>Added</td>
        <td>Resolved</td>
        <td>component:resolve</td>
    </tr>
    <tr>
        <td>Resolved</td>
        <td>Deployed</td>
        <td>component:deploy</td>
        <td rowspan="2">component:start</td>
    </tr>
    <tr>
        <td>Deployed</td>
        <td>Activated</td>
        <td>component:activate</td>
    </tr>
    <tr>
        <td>Activated</td>
        <td>Deployed</td>
        <td>component:deactivate</td>
        <td rowspan="2">component:stop</td>
    </tr>
    <tr>
        <td>Deployed</td>
        <td>Resolved</td>
        <td>component:undeploy</td>
    </tr>
    <tr>
        <td>Resolved</td>
        <td>Added</td>
        <td>component:unresolve</td>
        <td rowspan="2">component:uninstall</td>
    </tr>
    <tr>
        <td>Added</td>
        <td>—</td>
        <td>component:remove</td>
    </tr>
</table>

#### Activator <a name="usage-component-activator"></a>

Activators are module services that are invoked when a component is activated or deactivated. To create an activator,
follow these steps:

1. Implement the `ModuleActivator` interface in the module.
2. In the `module-info`, add `provides ... with ...`.

Important! The created activator will not be called if the configuration of the component does not specify that the
module is active:

```
<Module groupId="..." artifactId="..." version="..." active="true"/>
```
Thus, you can control through the component's configuration whether the module activators should be triggered or not.

When the component is activated, its activators are called in the order in which they are specified in the component's
configuration. During deactivation, they are called in reverse order (the last active module is deactivated first).

#### Configuration <a name="usage-component-config"></a>

The configuration can be set using a `Builder` or an XML.

```java
var config = ComponentConfig.builder()
        .name("foo")
        .version(Version.of("1.0.0"))
        .metadata("License", "Apache 2")
        .repositories(
            r -> r.name("local").url("file://...."),
            r -> r.name("central").url("https://repo1.maven.org/maven2/")
        )
        .parents(
            p -> p.name(...).version(...).versionMatch(...)
        )
        .modules(
            m -> m.groupId(...).artifactId(...).version(...).classifier(...).active(...)
                .directives(
                    d -> d.type(...).pkg(...).layer(...).module(...)
                )
        )
        .build();
```

A XML configuration template with all supported tags:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<Configuration name="foo" version="1.0.0" type="notBar">
    <Metadata>
        <Entry key="License" value="Apache 2"/>
    </Metadata>

    <Repositories>
        <Repository name="local" url="file://...."/>
        <Repository name="central" url="https://repo1.maven.org/maven2/"/>
    </Repositories>

    <Parents>
        <Parent name="... " version="..." versionMatch="major/minor/patch" optional="false"/>
    </Parents>

    <Choose>
        <When test="${info['os.family'] == 'linux'}">
            <Property name="modVersion" value="2.0.0"/>
        </When>
        <When test="${info['os.family'] == 'mac'}">
            <Property name="modVersion" value="3.0.0"/>
        </When>
    </Choose>

    <Modules>
        <If test="${...}">
            <Module groupId="..." artifactId="..." version="..."/>
        </If>

        <Module groupId="..." artifactId="..." version="${config['modVersion']}" active="true" nativeAccessEnabled="true">
            <Directives>
                <Directive type="opens/reads/exports" package="..." layer="..." module="..."/>
                <Directive type="requestsOpen/requestsRead/requestsExport" layer="..." module="..." package="..."/>
            </Directives>
        </Module>
    </Modules>
</Configuration>
```

For detailed information on the component configuration, refer to the `ComponentConfig` Javadoc. Here, we will
only cover the key points that should be noted.

**Expression Language**. All attributes can have EL expressions. By default, five maps are defined in the EL context:

```
sys — System.getProperties()
env — System.getenv()
config — This configuration properties
info — ComponentManager.getConfigInfo()
utils — ComponentManager.getConfigUtils
```

The last two maps are accessible through the `ComponentManager` and can be modified if necessary.

**Module Directive**. The module directives can be divided into two groups. The first group includes directives
specified in the `module-info`. The second group includes directives specified in the configuration.

Using directives in the configuration is necessary because, during module development, it is impossible to foresee
all possible use cases for the module. For example, if a module uses the API of a specification, it generally does
not know which implementation of that specification will be used. Therefore, when a particular implementation is used,
it may be necessary to add extra directives.

The layer attribute allows specifying the module's layer. If this attribute is not specified, the layer of this
component is used. To specify the framework and component layer, either the name `foo` or the name and version
`foo:1.0.0` is used. The name of the layer where the framework is located is `weaverbird-framework`.

The difference between the directives `opens`, `reads`, `exports` and the directives `requestsOpen`, `requestsRead`,
`requestsExport` is that the former are applied directly to the module specified in the `Module` tag, while the latter
are applied to the module specified in the `module` attribute. In other words, the first set of directives is applied
directly to the configured module, while the second set is applied to another module, usually from the parent layer.
For example, such a configuration:

```
<Directive type="requestsRead" layer="foo" module="bar"/>
```
will result in the `reads` directive being added to the `bar` module from `foo` layer.

It is important to note that directives are added through the layer controller. Since JPMS does not provide access
to the boot layer controller, `requests` directives cannot be applied to modules of the boot layer via the layer
controller.

However, this limitation can be worked around using a relay approach: the boot layer module opens its package to the
framework core module (configured via `--add-opens` at JVM startup), and since all layers are created by the core
module, it relays the access further to the target module using `Module.addOpens()`. For example, the following
configuration will open `java.time` from `java.base` to Gson:

```xml
<Module groupId="com.google.code.gson" artifactId="gson" version="${bom.gson.version}">
    <Directives>
        <Directive type="requestsOpen" layer="weaverbird-framework:${project.version}" module="java.base" package="java.time"/>
    </Directives>
</Module>
```

For a specific example of working with directives, see the configuration of the demo component — `weaverbird-gui`.

#### Events <a name="usage-component-events"></a>

The framework allows receiving events related to components. For this, `ModuleActivator` receives a `ModuleContext`,
from which a reference to the `Component` can be obtained. The `Component` allows adding a `ComponentObserver`. To
implement an observer, it is generally easier to use `AbstractComponentObserver`, which provides empty implementations
for all methods.

#### Services <a name="usage-component-services"></a>

Finding services within a single layer or in its parent layers is not an issue. However, when searching for services
that may not be in parent layers but instead in child layers or even outside the hierarchy, the task becomes more
complex. The reason for this is that such layers can be dynamically added and removed.

There are two solutions to this problem:

1. Use `ServiceTracker`.
2. Use `ComponentObserver`.

### Remote Control <a name="usage-remote-control"></a>

The framework provides out-of-the-box support for remote management over HTTP. To understand how to work with it, you
can refer to the integration test configuration in the `weaverbird-it-net` module.

It is important to note that the data channel is not encrypted. When deploying the client and server on different
hosts, you must ensure traffic protection using a VPN or an SSH tunnel.

### Text Commands <a name="usage-commands"></a>

Text commands are a powerful tool for working with the framework; however, their use is optional. It is important
to note that all commands, both built-in and custom, are automatically added to the CLI and GUI consoles.

All text-based commands are transmitted to the platform over the HTTP network protocol. On one hand, this introduces
a small overhead when the client and server run within the same JVM instance; on the other hand, this approach
allows interaction with different servers.

**Local and Remote Commands.** Any command can be local, remote, or both. Local commands can be executed without a
session, while remote commands require an active session.

**Command Executor**. The execution of commands is handled by `CommandExecutor`, which receives a `String` containing
the commands as input. Each command is a separate class implementing the `Command` interface. The executor splits the
text into individual commands and processes each one.

The execution flow of a local command is as follows:

```
Console ⮂ Command Executor ⮂ Command ⮂ Framework API
```

The execution flow of a remote command is as follows:

```
Console ⮂ Command Executor ⮂ Command ⮂ Client ⮂ Server ⮂ EndpointHandler ⮂ Framework API
```

**Custom Command**. To create a custom command, you need to do the following:

1. Implement the `Command` interface (it is recommended to inherit from `AbstractCommand`).
2. Create provider for `CommandService` in the module containing the command.
3. Add the module to the console component.

To create a custom `EndpointHandler`, do the following:
1. Implement the `EndpointHandler` interface.
2. Create provider for `EndpointHandlerService` in the module containing the handler.
3. Add the module to the server component.

**Command Scripts**. A command `String` can contain an unlimited number of commands separated by `;`. This allows for
the use of command scripts — files that contain text commands. Typically, scripts are stored in the `script` folder.

**Special Symbols**. When working with commands, the following special characters should be considered:

* `;` — used to separate commands.
* `#` — used for comments in scripts. It can only be the first character of a line.
* `!` — used as a prefix for commands when working in sessions, to execute them as local commands.

**Basic Commands**

```
# to open a session to the server
session:open demo -a 127.0.0.1:1200 -n admin -p admin

# to list all remote commands
command:list

# to list all local commands
!command:list

# to detach the session
!session:detach
```

### CLI <a name="usage-cli"></a>

The CLI is represented by a console that allows you to enter commands and parameters to interact with the framework.
The console supports command history, autocomplete (for both commands and parameters via the Tab key), and command
highlighting.

Unlike the GUI, the CLI is limited to executing commands. Its strength lies in its versatility — it can operate in
environments without a graphical interface.

### GUI <a name="usage-gui"></a>

The GUI offers significantly more capabilities compared to the CLI. While the CLI is limited to a single console,
the GUI provides three components: Console, Memory Log, and Diagrams — allowing you not only to execute commands but also
to view log messages and generate diagrams for working with layers, modules, and packages.

#### Console <a name="usage-gui-console"></a>

The Console is a component that allows executing text commands. It is similar to the CLI console with two main differences:

1. To invoke the completer, you need to use the `Ctrl` + `Space` keyboard shortcut.
2. It provides a graphical interface for working with sessions.

#### Memory Log <a name="usage-gui-memory-log"></a>

The memory log stores all log events that occur in the system. Since it receives all messages directly from `log4j2`
rather than reading them from a file, it offers greater flexibility for viewing and filtering those messages.

Currently, the memory log retains all messages until it is cleared, so it cannot be used in production. It is
intended solely for testing and debugging purposes.

By default, the memory log is not active. To enable it, you must set the parameter
`com.techsenger.weaverbird.core.log.memory` to `true` in the `.sh` / `.bat` scripts.

#### Diagrams <a name="usage-gui-diagrams"></a>

Diagrams are a useful tool that provides complete information about layers, modules, and packages. The importance of
this tool is especially heightened when working with complex systems.

Diagrams have their own settings, which can be viewed in the `Settings` dialog.

## Assembly Maven Plugin <a name="usage-assembly-plugin"></a>

The Assembly Maven Plugin is a key tool for working with the framework, as it handles all the work of assembling the
framework both for integration tests and distributions. It creates the initial directory structure with the repository
populated with the required modules and the necessary configuration files. The generated structure follows the standard
framework layout with `bin`, `config`, `repo`, and other directories. The `bin` directory contains `.sh` and `.bat`
scripts with the specified main class.

The plugin allows specifying custom modules that need to be added to the repository at the framework assembly stage.
Typically, this applies to modules that must reside in the boot layer, for example, if a module contains the framework
startup code. For such modules, `<onModulePath>` must be set to `true`.

It is important to note that if a module is used exclusively for components, it only needs to be specified in the
plugin configuration in special cases where the module cannot be resolved from any repository (including the local one)
during component resolution. In other words, as a rule, component modules do not need to be specified in the plugin
configuration.

The plugin provides the following goals:

* `assemble-runtime` — used for integration testing. It produces a minimal set of files required to run the framework.
  The execution is skipped if the specified `path` already exists.
* `assemble-dist` — creates a full distribution, including `.sh`/`.bat` scripts and the default Log4j2 configuration.
  The execution is skipped if the specified `path` already exists.
* `update` — intended for development purposes. It updates the repository with the specified modules in an existing
  distribution, avoiding full reassembly on every change. The execution is skipped if the specified `path` doesn't exist.

Common configuration parameters for `assemble-runtime` and `assemble-dist` goals:

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `path` | Yes | - | Path to the root framework directory where the runtime/distribution will be assembled |
| `components` | Yes | - | The list of components that will be added to the runtime/distribution |
| `components/component` | Yes | - | The component that will be added to the runtime/distribution |
| `modules` | No | - | The list of additional JPMS modules to resolve and include in the repository in addition to the minimal set of required modules. |
| `modules/module` | Yes | - | The JPMS module to resolve and include in the repository in addition to the minimal set of required modules. |
| `module/groupId` | Yes | - | Module group ID |
| `module/artifactId` | Yes | - | Module artifact ID |
| `module/version` | Yes | - | Module version |
| `module/type` | No | `jar` | Module type |
| `module/classifier` | No | - | Module Classifier |

Additional configuration parameters for the `assemble-dist` goal:

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `mainClass` | Yes | - | Main class in the format `module/fully.qualified.ClassName`, used only in `.sh`/`.bat` scripts|
| `scriptName` | No | `framework` | The name of the `.sh`/`.bat` scripts|
| `jvmArgs` | No | - | The JVM arguments used in `.sh`/`.bat` scripts|
| `jvmArgs/jvmArg` | Yes | - | The JVM argument used in `.sh`/`.bat` scripts|
| `module/onModulePath` | No | `false` | If `true`, the module will be added to the `--module-path` in `.sh`/`.bat` scripts |

Configuration parameters for `update` goal:

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `path` | Yes | - | Path to the root framework directory where the modules will be updated |
| `modules` | No | - | The list of modules in the repository to update. |
| `modules/module` | Yes | - | The module in the repository to update. |
| `module/groupId` | Yes | - | Module group ID |
| `module/artifactId` | Yes | - | Module artifact ID |
| `module/version` | Yes | - | Module version |
| `module/type` | No | `jar` | Module type |
| `module/classifier` | No | - | Module Classifier |

Example:

```xml
<plugin>
    <groupId>com.techsenger.weaverbird.assembly</groupId>
    <artifactId>weaverbird-assembly-maven-plugin</artifactId>
    <version>${weaverbird.version}</version>
    <executions>
        <execution>
            <phase>verify</phase>
            <goals>
                <goal>assemble-dist</goal>
            </goals>
            <configuration>
                <path>${project.build.directory}/framework</path>
                <mainClass>com.myapp/com.myapp.Main</mainClass>
                <jvmArgs>
                    <jvmArg>-agentlib:jdwp=transport=dt_socket,address=7700,server=y,suspend=n</jvmArg>
                </jvmArgs>
                <components>
                    <component>weaverbird-repo</component>
                </components>
                <modules>
                    <module>
                        <groupId>com.myapp</groupId>
                        <artifactId>myapp-core</artifactId>
                        <version>${myapp.version}</version>
                        <onModulePath>true</onModulePath>
                    </module>
                </modules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Requirements <a name="requirements"></a>

The framework requires Java 25+ and JavaFX 25+ for GUI console.

## Dependencies <a name="dependencies"></a>

This project will be available on Maven Central in a few weeks.

To work with the framework core, the following dependency is required. It provides the key APIs and SPIs for building
and managing modular components:

```
<dependency>
    <groupId>com.techsenger.weaverbird</groupId>
    <artifactId>weaverbird-core</artifactId>
    <version>${weaverbird.version}</version>
</dependency>
```

## Code Building <a name="code-building"></a>

To build the framework use standard Git and Maven commands:

    git clone https://github.com/techsenger/weaverbird
    cd weaverbird
    mvn clean install

## Running Demo <a name="running-demo"></a>

The project contains various demo modules. Some of them can be run using the Exec Maven Plugin, some only via
`.sh`/`.bat` scripts, and some support both options.

Information on how to run a specific `Demo.java` is provided in the Javadoc of that class.

## License <a name="license"></a>

Techsenger Weaverbird is licensed under the Apache License, Version 2.0.

## Contributing <a name="contributing"></a>

We welcome all contributions. You can help by reporting bugs, suggesting improvements, or submitting pull requests
with fixes and new features. If you have any questions, feel free to reach out — we’ll be happy to assist you.

## Support Us <a name="support-us"></a>

You can support our open-source work through [GitHub Sponsors](https://github.com/sponsors/techsenger).
Your contribution helps us maintain projects, develop new features, and provide ongoing improvements.
Multiple sponsorship tiers are available, each offering different levels of recognition and benefits.


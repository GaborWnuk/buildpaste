plugins {
	id("org.jetbrains.kotlin.jvm") version "2.4.0"
	id("net.neoforged.moddev") version "2.0.140"
}

val javaVersion = JavaVersion.VERSION_25
val mcVersionRangeForNeoForge: String = sc.properties["mod.mc_compat"]

group = property("mod.group").toString()
version = "${property("mod.version")}+${sc.current.version}"

base {
	archivesName = "${property("mod.id")}-neoforge"
}

repositories {
	maven("https://thedarkcolour.github.io/KotlinForForge/") { name = "KotlinForForge" }
	mavenCentral()
}

dependencies {
	implementation("thedarkcolour:kotlinforforge-neoforge:${property("deps.kotlin_forge")}")

	testImplementation(kotlin("test"))
}

neoForge {
	version = sc.properties["deps.neo_loader"]

	mods {
		register("buildpaste") {
			sourceSet(sourceSets.main.get())
		}
	}

	runs {
		register("client") {
			client()
			gameDirectory = rootProject.file("run")
		}
		register("server") {
			server()
			gameDirectory = rootProject.file("run")
		}
	}
}

tasks {
	processResources {
		inputs.property("minecraftVersionRange", mcVersionRangeForNeoForge)
		inputs.property("version", project.version)

		filesMatching("META-INF/neoforge.mods.toml") {
			expand(mapOf(
				"minecraftVersionRange" to inputs.properties["minecraftVersionRange"],
				"version" to inputs.properties["version"],
			))
		}
	}

	named("createMinecraftArtifacts") {
		dependsOn("stonecutterGenerate")
	}

	test {
		useJUnitPlatform()
	}

	jar {
		inputs.property("archivesName", project.base.archivesName)

		from(rootProject.file("LICENSE")) {
			rename { "${it}_${inputs.properties["archivesName"]}" }
		}
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(javaVersion.majorVersion.toInt())
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
	compilerOptions {
		jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(javaVersion.majorVersion)
	}
}

java {
	sourceCompatibility = javaVersion
	targetCompatibility = javaVersion

	toolchain {
		languageVersion = JavaLanguageVersion.of(javaVersion.majorVersion)
	}
}

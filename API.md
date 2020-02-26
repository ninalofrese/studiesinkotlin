## Hiding API key in Version Control (Optional but Recommended)

Once you have the API key, do the following steps to hide it in VCS.

- Add your key in **local.properties** present in the root folder.
- Get access to the key in **build.gradle** programmatically.
- Then the key is available to you in the program though **BuildConfig**.



```kotlin
//In local.properties
tmdb_api_key = "xxxxxxxxxxxxxxxxxxxxxxxxxx"

//In build.gradle (Module: app)
buildTypes.each {
        Properties properties = new Properties()
        properties.load(project.rootProject.file("local.properties").newDataInputStream())
        def tmdbApiKey = properties.getProperty("tmdb_api_key", "")

        it.buildConfigField 'String', "TMDB_API_KEY", tmdbApiKey
        
        it.resValue 'string', "api_key", tmdbApiKey
}

//Instead of defining api key for each build type, you can just do 
buildTypes.each {
	it.buildConfigField 'String', 'TMDB_API_KEY', TMDB_API_KEY
}

//In your Constants File
var tmdbApiKey = BuildConfig.TMDB_API_KEY
```

https://android.jlelse.eu/android-networking-in-2019-retrofit-with-kotlins-coroutines-aefe82c4d777
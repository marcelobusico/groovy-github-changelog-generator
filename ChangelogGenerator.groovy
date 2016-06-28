/**
 * Changelog Generator using Git and Github.
 * Create by Marcelo Busico on 2016-06-24.
 */

def getGithubUser() {
    def user = System.getenv("GITHUB_USERNAME")
    return user
}

def getGithubToken() {
    def token = System.getenv("GITHUB_TOKEN")
    return token
}

def getGithubApi() {
    def githubApi = System.getenv("GITHUB_API")
    if(!githubApi) {
	githubApi = "https://api.github.com"
    }
    return githubApi
}

def getResourceFromGithub(String repoApiUrl, String resource) {
    def url = "${repoApiUrl}${resource}"
    
    println "Getting resource from Github: '${url}'"
    
    def slurper = new groovy.json.JsonSlurper()
    def user = getGithubUser()
    def token = getGithubToken()        
    def encodedAuth = "${user}:${token}".getBytes().encodeBase64().toString()
	
    URLConnection connection = new URL(url).openConnection()
    connection.setRequestProperty("Authorization", "Basic ${encodedAuth}")
    def response = slurper.parse(new BufferedReader(new InputStreamReader(connection.getInputStream())))
    
    println "Done."
    println ""
    
    return response
}

def includePullInLabel(def prsByLabel, String label, def pr) {
    if(!prsByLabel[label]) {
        prsByLabel[label] = []
    }
    
    prsByLabel[label] += pr
    
    return prsByLabel
}

def executeGenerator() {
    println ""
    println "--------------------------------"
    println "Executing ChangeLog Generator..."
    println "--------------------------------"
    println ""

    

    if(!getGithubUser() || !getGithubToken()) {
	println "You need to define the following environment variables before using this script: GITHUB_USERNAME, GITHUB_TOKEN and optionally GITHUB_API."
	return
    }

    //Verify Arguments
    if(args.length != 3) {
	println "Usage: groovy ChangelogGenerator.groovy GITHUB_REPO_NAME TAG_START TAG_END"
	return
    }
    
    //Arguments
    def githubRepoName = args[0]
    def tagStart = args[1]
    def tagEnd = args[2]
    def githubApi = getGithubApi()
    def repoApiUrl = "${githubApi}/repos/${githubRepoName}"

    //Print environment data
    println "Repo API URL: ${repoApiUrl}"
    println "Start Tag: ${tagStart}"
    println "End Tag: ${tagEnd}"
    println ""


    //Get Local Git Data
    println "Getting commits from current local Git working copy..."
    def logCmd = "git log ${tagStart}..${tagEnd} --pretty=format:%H --merges"
    def logResult = logCmd.execute().text
    def commits = logResult.split("\\r?\\n")
    println "Done."
    println ""

    
    //Get GitHub Data
    def repoInfoResponse = getResourceFromGithub(repoApiUrl, "")
    def prResponse = getResourceFromGithub(repoApiUrl, "/pulls?state=closed")
    def issuesResponse = getResourceFromGithub(repoApiUrl, "/issues?state=closed")
    def tagsResponse = getResourceFromGithub(repoApiUrl, "/tags")
    

    def endTagCommitSha = null    
    for(def tag : tagsResponse) {
	if(tag.name.equals(tagEnd)) {
	    endTagCommitSha = tag.commit.sha
	    break
	}
    }    
    def tagCommitResponse = getResourceFromGithub(repoApiUrl, "/commits/${endTagCommitSha}")    
    def tagEndDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", tagCommitResponse.commit.author.date)

    
    //Include only pull requests for commits of this version.
    def versionPullRequests = []       
    for(def commitSha : commits) {
	for(def pr in prResponse) {
	    if(commitSha.equals(pr.merge_commit_sha)) {
	        versionPullRequests += pr
	    }
	}
    }

    def pullsByLabel = [:]
    
    //Group pull requests using labels
    for(def issue : issuesResponse) {
        if(issue.pull_request) {
	    //This issue is a pull request
	    for(def pr : versionPullRequests) {
	        if(pr.url.equals(issue.pull_request.url)) {
		    //This issue is for this pull request
		    
		    if(issue.labels) {
		        //Pull Request has labels
		        for(def label : issue.labels) {
			    includePullInLabel(pullsByLabel, "Merged Pull Requests - Label '${label.name}'", pr)
			}
		    } else {
		        //Pull Request does not have labels
		        includePullInLabel(pullsByLabel, "Merged Pull Requests", pr)
		    }
		}
	    }
	}
    }
    
    
    println "Processing changelog..."
    StringBuilder sb = new StringBuilder()

    sb.append("# Change Log")
    sb.append("\n\n")

    sb.append("## [${tagEnd}](${repoInfoResponse.html_url}/tree/${tagEnd}) (${tagEndDate.format('yyyy-MM-dd')})")
    sb.append("\n")

    sb.append("[Full Changelog](${repoInfoResponse.html_url}/compare/${tagStart}...${tagEnd})")
    sb.append("\n")

    for(def pulls : pullsByLabel) {
	sb.append("\n")
	sb.append("**${pulls.key}:**")
	sb.append("\n\n")

	for(def pr : pulls.value) {
	    sb.append("- ${pr.title} [\\#${pr.number}](${pr.html_url}) ([${pr.user.login}](${pr.user.html_url}))")
	    sb.append("\n")
	}      
    }

    String changeLog = sb.toString()
    println "Done."



    println ""
    println ""
    println "Generated Changelog:"
    println "--------------------"
    println ""
    println changeLog
    println "--------------------"
    println ""
    println ""


    def changelogFile = new File('CHANGELOG.md')
    String currentContent = null
    if(changelogFile.exists()) {
	println "Reading existing CHANGELOG.md file..."
	currentContent = changelogFile.getText()
	println "Done."
	println ""
    }



    println "Writing CHANGELOG.md file..."
    changelogFile.write(changeLog)
    if(currentContent) {
	changelogFile.append("\n")
	currentContent.eachLine { line, count ->
	    if (count > 1) {
		changelogFile.append(line)
		changelogFile.append("\n")
	    }
	}
    }
    println "Done."    



    println ""
    println "-------------------------------"
    println "Changelog generation completed."
    println "-------------------------------"
    println ""
}

executeGenerator()

import sys
import requests

gitlab_api_url = sys.argv[1]
project_id = sys.argv[2]
access_token = sys.argv[3]

def fetch_release_notes():
    commits = make_request("repository/commits")
    if not commits:
        return "No changes detected"

    release_notes = ""
    if is_merge_commit(commits[0]):
        release_notes = commits[0]["message"]
    else:
        last_master_build_commit = fetch_last_master_build_commit()
        commit_index = next((index for index, element in enumerate(commits) if element == last_master_build_commit), 0)
        for i in range(commit_index):
            release_notes += commits[i]["message"]

    if not release_notes:
        return "No changes detected"
    else:
        return release_notes

def fetch_last_master_build_commit():
    builds = make_request("jobs?scope=success&per_page=30")
    last_master_build = next((element for element in builds if element["name"] == "master"), None)

    last_master_build_commit = None
    if last_master_build != None:
        last_master_build_commit = last_master_build["commit"]

    return last_master_build_commit

def is_merge_commit(commit):
    return len(commit["parent_ids"]) > 1

def make_request(path):
    headers = {"PRIVATE-TOKEN": access_token}
    endpoint = f"{gitlab_api_url}/projects/{project_id}/{path}"
    response = requests.get(endpoint, headers=headers).json()
    return response

print(fetch_release_notes())

import subprocess

def main():
    print("Running force push...")
    res = subprocess.run(["git", "push", "-f", "origin", "jules-2025067850680877639-61f6d10d"], capture_output=True, text=True)
    print("STDOUT:", res.stdout)
    print("STDERR:", res.stderr)
    print("RC:", res.returncode)

if __name__ == "__main__":
    main()

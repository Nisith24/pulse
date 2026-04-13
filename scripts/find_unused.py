import os
import re

def get_kt_files(directory):
    kt_files = []
    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith('.kt'):
                kt_files.append(os.path.join(root, file))
    return kt_files

def main():
    kt_files = get_kt_files('app/src/main/java')

    # Extract class names and file names
    # Create a map of filename (without .kt) to file path
    file_map = {}
    for f in kt_files:
        name = os.path.basename(f)[:-3]
        file_map[name] = f

    # Read all contents
    all_text = ""
    for f in kt_files:
        with open(f, 'r', encoding='utf-8') as file:
            all_text += file.read() + "\n"

    unused = []
    for name, path in file_map.items():
        if name in ["MainActivity", "PulseApp", "AppModule", "Theme", "Routes", "AppDatabase"]:
            continue
        # check if name appears in other files (very naive approach)
        # we can just count occurrences of name in all_text
        # and if it's only imported/used in its own file
        count = len(re.findall(r'\b' + re.escape(name) + r'\b', all_text))

        # We need to count occurrences outside its own file
        # A better way is to check each file

        used = False
        for f in kt_files:
            if f == path:
                continue
            with open(f, 'r', encoding='utf-8') as file:
                content = file.read()
                if re.search(r'\b' + re.escape(name) + r'\b', content):
                    used = True
                    break
        if not used:
            unused.append(path)

    print("Potentially unused files (no exact word match in other files):")
    for u in unused:
        print(u)

if __name__ == "__main__":
    main()

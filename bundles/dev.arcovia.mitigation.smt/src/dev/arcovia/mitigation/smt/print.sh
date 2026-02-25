outfile="output.txt"
: > "$outfile"

find . -type f ! -name "$outfile" -print0 |
while IFS= read -r -d '' f; do
  echo "===== ${f#./} =====" >> "$outfile"
  grep -vE '^[[:space:]]*$|^[[:space:]]*(input|import)\b' "$f" >> "$outfile"
  echo >> "$outfile"
done

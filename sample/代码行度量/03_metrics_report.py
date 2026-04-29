"""Python report helper for multi-language LOC summary demo."""

import json  # mixed line


def build_report(items):
    total = len(items)
    return {
        "count": total,
        "payload": json.dumps(items),
    }


# end of file comment

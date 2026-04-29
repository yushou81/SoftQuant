#include <string>
#include <vector>

// C++ sample for language summary verification.
int sumPositive(const std::vector<int>& values) {
    int total = 0;
    for (int value : values) {
        if (value > 0) {
            total += value;
        }
    }
    return total;
}

#ifndef ANDROIDMDP_TYPES_HPP
#define ANDROIDMDP_TYPES_HPP

#include <vector>
#include <unordered_map>

#include <boost/multi_array.hpp>

#include <Eigen/Core>
#include <Eigen/SparseCore>

namespace AndroidMDP
{
    using Table3D = boost::multi_array<double, 3>;
    using Table2D = boost::multi_array<double, 2>;

    using Matrix2D = Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor | Eigen::AutoAlign>;
    using SparseMatrix2D = Eigen::SparseMatrix<double, Eigen::RowMajor>;
    using SparseMatrix2DLong = Eigen::SparseMatrix<long, Eigen::RowMajor>;

    using Vector = Eigen::Matrix<double, Eigen::Dynamic, 1>;

    using Matrix3D = std::vector<Matrix2D>;
    using SparseMatrix3D = std::vector<SparseMatrix2D>;
    using SparseMatrix3DLong = std::vector<SparseMatrix2DLong >;

    using Matrix4D = boost::multi_array<Matrix2D, 2>;
    using SparseMatrix4D = boost::multi_array<SparseMatrix2D, 2>;

    using ProbabilityVector = Vector;

    template<typename T>
    struct remove_cv_ref {using type = typename std::remove_cv<typename std::remove_reference<T>::type>::type; };
}

#endif
